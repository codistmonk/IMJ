package imj3.tools;

import static java.lang.Math.*;
import static java.util.Arrays.fill;
import static net.sourceforge.aprog.swing.SwingTools.*;
import static net.sourceforge.aprog.tools.Tools.*;
import imj3.core.Channels;
import imj3.core.Image2D;
import imj3.processing.Pipeline;
import imj3.processing.Pipeline.ClassDescription;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.xml.XMLTools;

import org.w3c.dom.Document;

/**
 * @author codistmonk (creation 2015-04-04)
 */
public final class GroundTruth2Bin {
	
	private GroundTruth2Bin() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 * @throws IOException 
	 */
	public static final void main(final String[] commandLineArguments) throws IOException {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String pipelinePath = arguments.get("pipeline", "");
		// TODO later, retrieve training set from pipeline instead
		final String groundTruthPath = arguments.get("zip", "");
		final String outputPath = baseName(groundTruthPath) + ".bin";
		
		if (!new File(outputPath).exists()) {
			final int lod = arguments.get("lod", 4)[0];
			final Pipeline pipeline = XMLSerializable.objectFromXML(new File(pipelinePath));
			final Map<Integer, Area> regions;
			
			try (final MultifileSource source = new MultifileSource(groundTruthPath);
					final InputStream input = source.getInputStream("annotations.xml")) {
				final Document xml = XMLTools.parse(input);
				
				regions = XMLSerializable.objectFromXML(xml.getDocumentElement(), new HashMap<>());
			}
			
			final String imagePath = groundTruthPath.substring(0, groundTruthPath.indexOf("_groundtruth_")) + ".zip";
			final Image2D image = IMJTools.read(imagePath, lod);
			final Channels channels = image.getChannels();
			final int channelCount = channels.getChannelCount();
			final int patchSize = 32;
			final int planeSize = patchSize * patchSize;
			final byte[] buffer = new byte[1 + planeSize * channelCount];
			final Map<String, Long> counts = new LinkedHashMap<>();
			final int imageWidth = image.getWidth();
			final int imageHeight = image.getHeight();
			final double scale = pow(2.0, -lod);
			
			debugPrint(imageWidth, imageHeight, channels);
			
			try (final OutputStream output = new FileOutputStream(outputPath)) {
				for (final ClassDescription classDescription : pipeline.getClassDescriptions()) {
					final Area region = regions.get(classDescription.getLabel());
					
					region.transform(AffineTransform.getScaleInstance(scale, scale));
					
					final Rectangle regionBounds = region.getBounds();
					final int regionWidth = regionBounds.width;
					final int regionHeight = regionBounds.height;
					final BufferedImage mask = new BufferedImage(regionWidth, regionHeight, BufferedImage.TYPE_BYTE_BINARY);
					
					debugPrint(classDescription, regionBounds);
					
					{
						final Graphics2D graphics = mask.createGraphics();
						
						graphics.setColor(Color.WHITE);
						graphics.translate(-regionBounds.x, -regionBounds.y);
						graphics.fill(region);
						graphics.dispose();
					}
					
					buffer[0] = (byte) pipeline.getClassDescriptions().indexOf(classDescription);
					long count = 0L;
					
					for (int y = 0; y < regionHeight; ++y) {
						final int top = regionBounds.y + y - patchSize / 2;
						final int bottom = min(top + patchSize, imageHeight);
						
						for (int x = 0; x < regionWidth; ++x) {
							if ((mask.getRGB(x, y) & 0x00FFFFFF) != 0) {
								final int left = regionBounds.x + x - patchSize / 2;
								final int right = min(left + patchSize, imageWidth);
								
								fill(buffer, 1, buffer.length, (byte) 0);
								
								for (int yy = max(0, top); yy < bottom; ++yy) {
									for (int xx = max(0, left); xx < right; ++xx) {
										final long pixelValue = image.getPixelValue(xx, yy);
										
										for (int channelIndex = 0; channelIndex < channelCount; ++channelIndex) {
											buffer[1 + planeSize * channelIndex + (yy - top) * patchSize + (xx - left)] = (byte) channels.getChannelValue(pixelValue, channelIndex);
										}
									}
								}
								
								output.write(buffer);
								
								++count;
							}
						}
					}
					
					counts.put(classDescription.toString(), count);
				}
			}
			
			debugPrint(counts);
		}
		
		BinView.main(outputPath);
	}
	
	public static final byte[] read(final String path) {
		try (final InputStream input = new FileInputStream(path);
				final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			write(input, output);
			
			return output.toByteArray();
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	/**
	 * @author codistmonk (creation 2015-04-04)
	 */
	public static final class BinView extends JPanel {
		
		private static final long serialVersionUID = -5316982568644236740L;
		
		private final JTable table;
		
		public BinView(final byte[] data, final int itemWidth, final int itemHeight, final int itemChannelCount) {
			super(new BorderLayout());
			final int rowSize = 1 + itemWidth * itemHeight * itemChannelCount;
			final DefaultTableModel model = new DefaultTableModel(array("#", "class", "datum"), data.length / rowSize) {
				
				@Override
				public final Class<?> getColumnClass(final int columnIndex) {
					return 2 == columnIndex ? DUMMY.getClass() : super.getColumnClass(columnIndex);
				}
				
				private static final long serialVersionUID = 44752070418830499L;
				
			};
			this.table = new JTable(model);
			
			for (int i = 0; i < data.length; i += rowSize) {
				model.setValueAt(i / rowSize, i / rowSize, 0);
				model.setValueAt(data[i] & 0xFF, i / rowSize, 1);
				model.setValueAt(DUMMY, i / rowSize, 2);
			}
			
			SwingTools.setCheckAWT(false);
			
			try {
				this.add(scrollable(this.table), BorderLayout.CENTER);
			} finally {
				SwingTools.setCheckAWT(true);
			}
			
			this.table.setDefaultRenderer(DUMMY.getClass(), new TableCellRenderer() {
				
				private final BufferedImage image = new BufferedImage(itemWidth, itemHeight, BufferedImage.TYPE_3BYTE_BGR);
				
				private final JLabel label = new JLabel(new ImageIcon(this.image));
				
				@Override
				public final Component getTableCellRendererComponent(final JTable table, final Object value,
						final boolean isSelected, final boolean hasFocus, final int row, final int column) {
					
					for (int y = 0; y < itemHeight; ++y) {
						for (int x = 0; x < itemWidth; ++x) {
							int rgb = ~0;
							
							for (int i = 0; i < itemChannelCount; ++i) {
								rgb = (rgb << 8) | (data[row * rowSize + 1 + itemWidth * itemHeight * i + y * itemWidth + x] & 0xFF);
							}
							
							image.setRGB(x, y, rgb);
						}
					}
					
					return this.label;
				}
				
			});
			
			this.table.setRowHeight(itemHeight + 2);
			
			this.table.setDropTarget(new DropTarget() {
				
				@Override
				public final synchronized void drop(final DropTargetDropEvent event) {
					for (final File file : getFiles(event)) {
						main(file.getPath());
					}
				}
				
				private static final long serialVersionUID = -8699426509985335278L;
				
			});
		}
		
		private static final byte[] DUMMY = new byte[0];
		
		/**
		 * @param commandLineArguments
		 * <br>Must not be null
		 */
		public static final void main(final String... commandLineArguments) {
			final String path = commandLineArguments[0];
			
			SwingTools.show(new BinView(read(path), 32, 32, 3), path, false);
		}
		
	}
	
}
