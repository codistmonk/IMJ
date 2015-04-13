package imj3.draft;

import static imj3.tools.IMJTools.read;
import static net.sourceforge.aprog.swing.SwingTools.getFiles;
import static net.sourceforge.aprog.tools.Tools.*;

import imj3.core.Channels;
import imj3.core.IMJCoreTools;
import imj3.core.Image2D;
import imj3.tools.AwtImage2D;
import imj3.tools.Image2DComponent;
import imj3.tools.Image2DComponent.Overlay;
import imj3.tools.Image2DComponent.TileOverlay;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Composite;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import net.sourceforge.aprog.swing.ScriptingPanel;
import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.CommandLineArgumentsParser;
import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.Launcher;

/**
 * @author codistmonk (creation 2015-04-08)
 */
public final class CaffeinatedAnalysis {
	
	private CaffeinatedAnalysis() {
		throw new IllegalInstantiationException();
	}
	
	static final Preferences preferences = Preferences.userNodeForPackage(CaffeinatedAnalysis.class);
	
	public static final AtomicInteger patchSize = new AtomicInteger(32);
	
	public static final AtomicInteger patchStride = new AtomicInteger(8);
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String[] commandLineArguments) {
		ScriptingPanel.openScriptingPanelOnCtrlF2();
		
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("file", preferences.get("image.path", ""));
		debugPrint(imagePath);
		final Image2D image;
		
		if (imagePath.isEmpty()) {
			image = new AwtImage2D("", new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
		} else {
			image = read(imagePath, 0);
		}
		
		final JPanel mainPanel = new JPanel(new BorderLayout());
		
		mainPanel.setDropTarget(new DropTarget() {
			
			@Override
			public final synchronized void drop(final DropTargetDropEvent event) {
				final String imagePath = getFiles(event).get(0).getPath();
				final Image2DComponent newComponent = new Image2DComponent(read(imagePath, 0));
				
				newComponent.setOverlay(new Overlay() {
					
					@Override
					public final void update(final Graphics2D graphics, final Rectangle region) {
						graphics.drawString(Double.toString(newComponent.getImage().getScale()), 0, region.height);
					}
					
					private static final long serialVersionUID = -3450896552565525297L;
					
				});
				
				mainPanel.remove(0);
				mainPanel.add(newComponent, BorderLayout.CENTER);
				mainPanel.validate();
				
				((Frame) SwingUtilities.getWindowAncestor(mainPanel)).setTitle(imagePath);
				
				preferences.put("image.path", imagePath);
			}
			
			private static final long serialVersionUID = 2354002882066104716L;
			
		});
		
		{
			final Image2DComponent imageView = new Image2DComponent(image);
			
			imageView.setOverlay(new Overlay() {
				
				@Override
				public final void update(final Graphics2D graphics, final Rectangle region) {
					graphics.drawString(Double.toString(imageView.getImage().getScale()), 0, region.height);
				}
				
				private static final long serialVersionUID = -3450896552565525297L;
				
			});
			
			mainPanel.add(imageView, BorderLayout.CENTER);
		}
		
		mainPanel.setFocusable(true);
		mainPanel.addKeyListener(new KeyAdapter() {
			
			private Collection<String> cacheKeys = new HashSet<>();
			
			@Override
			public final void keyPressed(final KeyEvent event) {
				final Image2DComponent imageView = (Image2DComponent) mainPanel.getComponent(0);
				
				if (event.getKeyCode() == KeyEvent.VK_SPACE) {
					final Image2D image = imageView.getImage();
					debugPrint(image.getScale(), image.getPixelCount());
				}
				
				if (event.getKeyCode() == KeyEvent.VK_ENTER) {
					process(imageView, this.cacheKeys);
				}
			}
			
		});
		
		SwingTools.show(mainPanel, imagePath.isEmpty() ? "Drop an image file to open it" : imagePath, false);
		
		mainPanel.requestFocusInWindow();
	}
	
	public static final void process(final Image2DComponent view, final Collection<String> cacheKeys) {
		new SwingWorker<Void, Void>() {
			
			private BufferedImage mask = null;
			
			@Override
			protected final Void doInBackground() throws Exception {
				this.mask = CaffeinatedAnalysis.process(view.getImage());
				
				return null;
			}
			
			@Override
			protected final void done() {
				cacheKeys.forEach(IMJCoreTools::uncache);
				
				final BufferedImage mask = this.mask;
				final int maskWidth = mask.getWidth();
				final int maskHeight = mask.getHeight();
				
				view.setTileOverlay(new TileOverlay() {
					
					@Override
					public final void update(final Graphics2D graphics, final Point tileXY, final Rectangle region) {
						final Image2D image = view.getImage();
						final int imageWidth = image.getWidth();
						final int imageHeight = image.getHeight();
						final String key = image.getTileKey(tileXY.x, tileXY.y) + "_overlay";
						final BufferedImage overlayTile = IMJCoreTools.cache(key, () -> {
							final Image2D tile = image.getTile(tileXY.x, tileXY.y);
							final int tileWidth = tile.getWidth();
							final int tileHeight = tile.getHeight();
							final BufferedImage result = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_3BYTE_BGR);
							final Graphics2D overlayGraphics = result.createGraphics();
							
							overlayGraphics.drawImage(mask, 0, 0, tileWidth, tileHeight,
									tileXY.x * maskWidth / imageWidth, tileXY.y * maskHeight / imageHeight,
									(tileXY.x + tileWidth) * maskWidth / imageWidth, (tileXY.y + tileHeight) * maskHeight / imageHeight, null);
							overlayGraphics.dispose();
							
							return result;
						});
						
						cacheKeys.add(key);
						
						final Composite composite = graphics.getComposite();
						
						graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3F));
						graphics.drawImage(overlayTile, region.x, region.y, region.width, region.height, null);
						graphics.setComposite(composite);
					}
					
					private static final long serialVersionUID = 8011571177140912595L;
					
				});
				
				view.repaint();
			}
			
		}.execute();
	}
	
	public static final BufferedImage process(final Image2D image) {
		final String userHome = System.getProperty("user.home");
		final String caffe = userHome + "/workspace/caffe-master";
		final byte[] buffer = new byte[3073];
		final File dataFile = new File("/dev/shm/imj3.data.bin");
		final File labelsFile = new File("/dev/shm/imj3.labels.bin");
		final Channels channels = image.getChannels();
		final int imageWidth = image.getWidth();
		final int imageHeight = image.getHeight();
		final int patchSize = CaffeinatedAnalysis.patchSize.get();
		final int patchStride = CaffeinatedAnalysis.patchStride.get();
		final int planeSize = patchSize * patchSize;
		long n = 0;
		
		try {
			try (final OutputStream output = new FileOutputStream(dataFile)) {
				debugPrint("Writing", dataFile);
				
				for (int y = patchSize / 2; y < imageHeight; y += patchStride) {
					final int top = y - patchSize / 2;
					final int bottom = top + patchSize;
					for (int x = patchSize / 2; x < imageWidth; x += patchStride) {
						final int left = x - patchSize / 2;
						final int right = left + patchSize;
						
						for (int yy = top; yy < bottom; ++yy) {
							for (int xx = left; xx < right; ++xx) {
								final int i = 1 + (yy - top) * patchSize + (xx - left);
								if (0 <= yy && yy < imageHeight && 0 <= xx && xx < imageWidth) {
									final long pixelValue = image.getPixelValue(xx, yy);
									
									for (int channelIndex = 0; channelIndex < 3; ++channelIndex) {
										buffer[i + planeSize * channelIndex] = (byte) channels.getChannelValue(pixelValue, channelIndex);
									}
								} else {
									for (int channelIndex = 0; channelIndex < 3; ++channelIndex) {
										buffer[i + planeSize * channelIndex] = 0;
									}
								}
							}
						}
						
						output.write(buffer);
						++n;
					}
				}
				
				output.close();
			}
			
			debugPrint(n);
			
//			GroundTruth2Bin.BinView.main(dataFile.getPath());
			
			final Process process = Runtime.getRuntime().exec(
					array("Debug/RunCaffe", "test.prototxt", "test.caffemodel", dataFile.getPath(), labelsFile.getPath()),
					array("LD_LIBRARY_PATH=$LD_LIBRARY_PATH:" + caffe + "/lib:" + caffe + "/build/lib", "GLOG_logtostderr=1"),
					new File("../RunCaffe"));
			
			try (final OutputStream processControl = process.getOutputStream()) {
				Launcher.redirectOutputsToConsole(process);
				
				processControl.close();
				
				debugPrint(process.waitFor());
			}
			
			final BufferedImage result = new BufferedImage(
					(imageWidth - patchSize / 2 + patchStride - 1) / patchStride,
					(imageHeight - patchSize / 2 + patchStride - 1) / patchStride, BufferedImage.TYPE_BYTE_BINARY);
			final byte[] labelsBuffer = new byte[result.getWidth() * result.getHeight()];
			
			debugPrint(labelsBuffer.length);
			
			try (final InputStream input = new FileInputStream(labelsFile)) {
				input.read(labelsBuffer);
			}
			
			for (int y = 0, p = 0; y < result.getHeight(); ++y) {
				for (int x = 0; x < result.getWidth(); ++x, ++p) {
					result.setRGB(x, y, labelsBuffer[p] != 0 ? ~0 : 0);
				}
			}
			
			return result;
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		} catch (final Exception exception) {
			throw unchecked(exception);
		} finally {
			dataFile.delete();
			labelsFile.delete();
		}
	}
	
}
