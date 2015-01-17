package imj3.draft;

import static imj3.core.Channels.Predefined.blue8;
import static imj3.core.Channels.Predefined.green8;
import static imj3.core.Channels.Predefined.red8;
import static imj3.draft.VisualSegmentation.setSharedProperty;
import static java.lang.Math.abs;
import static net.sourceforge.aprog.swing.SwingTools.scrollable;
import static net.sourceforge.aprog.tools.Tools.cast;
import imj2.tools.Canvas;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-01-16)
 */
public final class TrainableSegmentation {
	
	private TrainableSegmentation() {
		throw new IllegalInstantiationException();
	}
	
	public static final String PALETTE_XML = "palette.xml";
	
	static final Preferences preferences = Preferences.userNodeForPackage(TrainableSegmentation.class);
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public final void run() {
				final JFrame mainFrame = new JFrame();
				final Component[] view = { null };
				final JTree tree = newQuantizerTree();
				
				mainFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				
				mainFrame.setPreferredSize(new Dimension(512, 512));
				mainFrame.add(scrollable(tree), BorderLayout.WEST);
				
				setSharedProperty(mainFrame, "tree", tree);
				
				mainFrame.setDropTarget(new DropTarget() {
					
					@Override
					public final synchronized void drop(final DropTargetDropEvent event) {
						final File file = SwingTools.getFiles(event).get(0);
						
						setView(mainFrame, view, file);
						
						preferences.put("filePath", file.getPath());
					}
					
					/**
					 * {@value}.
					 */
					private static final long serialVersionUID = 5442000733451223725L;
					
				});
				
				try {
					mainFrame.add(SwingTools.scrollable(new ImageComponent(ImageIO.read(new File("/Users/Greg/Desktop/sysimit/sysimit_nb_cd8/lod4/SYS_NB01_97_00100_3_005_lod4.jpg")))));
				} catch (final IOException exception) {
					exception.printStackTrace();
				}
				
				SwingTools.packAndCenter(mainFrame).setVisible(true);
			}
			
		});
	}
	
	public static final void setView(final JFrame mainFrame, final Component[] view, final File file) {
		// TODO
	}
	
	public static final JTree newQuantizerTree() {
		return new JTree(new DefaultTreeModel(new Quantizer()));
	}
	
	/**
	 * @author codistmonk (creation 2015-01-16)
	 */
	public static final class ImageComponent extends JComponent {
		
		private final BufferedImage image;
		
		private final Canvas buffer;
		
		public ImageComponent(final BufferedImage image) {
			this.image = image;
			final int imageWidth = image.getWidth();
			final int imageHeight = image.getHeight();
			this.buffer = new Canvas().setFormat(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
			
			this.setMinimumSize(new Dimension(imageWidth, imageHeight));
			this.setMaximumSize(new Dimension(imageWidth, imageHeight));
			this.setPreferredSize(new Dimension(imageWidth, imageHeight));
			this.setSize(new Dimension(imageWidth, imageHeight));
		}
		
		public final BufferedImage getImage() {
			return this.image;
		}
		
		@Override
		protected final void paintComponent(final Graphics g) {
			super.paintComponent(g);
			
			// XXX somehow this prevents a defect of Java 8 on Mac OS X Lion with low graphic capabilities
			this.buffer.getGraphics().drawImage(this.getImage(), 0, 0, null);
			
			g.drawImage(this.buffer.getImage(), 0, 0, null);
		}
		
		private static final long serialVersionUID = 1260599901446126551L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-01-16)
	 */
	public static abstract class QuantizerNode extends DefaultMutableTreeNode {
		
		private static final long serialVersionUID = 7636724853656189383L;
		
		/**
		 * @author codistmonk (creation 2015-01-16)
		 */
		public static abstract interface Visitor<V> extends Serializable {
			
			public abstract V visit(Quantizer quantizer);
			
			public abstract V visit(QuantizerCluster cluster);
			
			public abstract V visit(QuantizerPrototype prototype);
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-01-16)
	 */
	public static final class Quantizer extends DefaultMutableTreeNode {
		
		private int[] buffer = new int[1];
		
		public final int getScale() {
			return this.buffer.length;
		}
		
		public final void setScale(final int scale) {
			if (scale <= 0) {
				throw new IllegalArgumentException();
			}
			
			if (scale != this.getScale()) {
				this.buffer = new int[scale];
			}
		}
		
		public final String getEditScale() {
			return Integer.toString(this.getScale());
		}
		
		public final void parseEditScale(final String editScale) {
			this.setScale(Integer.parseInt(editScale));
		}
		
		public final QuantizerCluster quantize(final BufferedImage image, final int x, final int y) {
			extractValues(image, x, y, this.getScale(), this.buffer);
			final int n = this.getChildCount();
			QuantizerCluster result = null;
			double bestDistance = Double.POSITIVE_INFINITY;
			
			for (int i = 0; i < n; ++i) {
				final QuantizerCluster cluster = (QuantizerCluster) this.getChildAt(i);
				final double distance = cluster.distanceTo(this.buffer);
				
				if (distance < bestDistance) {
					result = cluster;
					bestDistance = distance;
				}
			}
			
			return result;
		}
		
		private static final long serialVersionUID = 3228746395868315788L;
		
		public static final int[] extractValues(final BufferedImage image, final int x, final int y, final int patchSize, final int[] result) {
			Arrays.fill(result, 0);
			
			final int width = image.getWidth();
			final int height = image.getHeight();
			final int s = patchSize / 2;
			final int left = x - s;
			final int right = left + patchSize;
			final int top = y - s;
			final int bottom = top + patchSize;
			
			for (int yy = top, i = 0; yy < bottom; ++yy) {
				if (0 <= yy && yy < height) {
					for (int xx = left; xx < right; ++xx, ++i) {
						if (0 <= xx && xx < width) {
							result[i] = image.getRGB(xx, yy);
						}
					}
				} else {
					i += patchSize;
				}
			}
			
			return result;
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-01-16)
	 */
	public static final class QuantizerCluster extends DefaultMutableTreeNode {
		
		private String name = "cluster";
		
		private int label = 1;
		
		public final String getName() {
			return this.name;
		}
		
		public final QuantizerCluster setName(final String name) {
			this.name = name;
			
			return this;
		}
		
		public final int getLabel() {
			return this.label;
		}
		
		public final QuantizerCluster setLabel(final int label) {
			this.label = label;
			
			return this;
		}
		
		public final String getEditLabel() {
			return "#" + Integer.toHexString(this.label).toUpperCase(Locale.ENGLISH);
		}
		
		public final void parseEditLabel(final String editLabel) {
			this.setLabel(editLabel.startsWith("#") ? (int) Long.parseLong(editLabel.substring(1), 16) : Integer.parseInt(editLabel));
		}
		
		public final double distanceTo(final int[] values) {
			final int n = this.getChildCount();
			double result = Double.POSITIVE_INFINITY;
			
			for (int i = 0; i < n; ++i) {
				final double distance = ((QuantizerPrototype) this.getChildAt(i)).distanceTo(values);
				
				if (distance < result) {
					result = distance;
				}
			}
			
			return result;
		}
		
		private static final long serialVersionUID = -3727849715989585298L;
		
	}
	
	/**
	 * @author codistmonk (creation 2015-01-16)
	 */
	public static final class QuantizerPrototype extends DefaultMutableTreeNode {
		
		private int[] data = new int[0];
		
		public final int[] getData() {
			final Quantizer root = cast(Quantizer.class, this.getRoot());
			
			if (root == null) {
				return null;
			}
			
			if (this.data.length != root.getScale()) {
				this.data = new int[root.getScale()];
			}
			
			return this.data;
		}
		
		public final String getEditData() {
			return Tools.join(",", this.getData());
		}
		
		public final void parseEditData(final String editData) {
			System.arraycopy(Arrays.stream(editData.split(",")).mapToInt(Integer::parseInt), 0,
					this.getData(), 0, this.getData().length);
		}
		
		public final double distanceTo(final int[] values) {
			final int n = values.length;
			
			if (n != this.data.length) {
				throw new IllegalArgumentException();
			}
			
			double result = 0.0;
			
			for (int i = 0; i < n; ++i) {
				final int thisRGB = this.data[i];
				final int thatRGB = values[i];
				result += abs(red8(thisRGB) - red8(thatRGB))
						+ abs(green8(thisRGB) - green8(thatRGB))
						+ abs(blue8(thisRGB) - blue8(thatRGB));
						
			}
			
			return result;
		}
		
		private static final long serialVersionUID = 946728342547485375L;
		
	}
	
}
