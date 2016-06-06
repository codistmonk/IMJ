package imj3.draft;

import static imj3.core.IMJCoreTools.cache;
import static imj3.tools.IMJTools.forEachPixelInEachComponent4;
import static imj3.tools.IMJTools.forEachTile;
import static imj3.tools.SVS2Multifile.newMetadata;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static multij.swing.SwingTools.horizontalBox;
import static multij.swing.SwingTools.show;
import static multij.tools.Tools.baseName;
import static multij.tools.Tools.debugPrint;

import imj3.core.Channels;
import imj3.core.IMJCoreTools;
import imj3.core.IMJCoreTools.Reference;
import imj3.core.Image2D;
import imj3.core.Image2D.Pixel2DProcessor;
import imj3.tools.AwtImage2D;
import imj3.tools.IMJTools;
import imj3.tools.IMJTools.ComponentComembership;
import imj3.tools.Image2DComponent;
import imj3.tools.Image2DComponent.Overlay;
import imj3.tools.Image2DComponent.TileOverlay;
import imj3.tools.SVS2Multifile;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import multij.tools.Canvas;
import multij.tools.CommandLineArgumentsParser;
import multij.tools.IllegalInstantiationException;
import multij.xml.XMLTools;

/**
 * @author codistmonk (creation 2016-06-03)
 */
public final class QuickSeg2 {
	
	private QuickSeg2() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String... commandLineArguments) {
		final CommandLineArgumentsParser arguments = new CommandLineArgumentsParser(commandLineArguments);
		final String imagePath = arguments.get("image", "");
		
		if (!imagePath.isEmpty()) {
			final int q = arguments.get("q", 5)[0];
			final int s = arguments.get("s", 16)[0];
			final Image2D image = IMJTools.read(imagePath);
			
			segmentFull(image, q, s, baseName(imagePath) + "_segments.zip", Monitor.DEFAULT);
			
			return;
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public final void run() {
				final JPanel mainPanel = new JPanel(new BorderLayout());
				final JTextField qField = new JTextField("6");
				final JTextField sField = new JTextField("32");
				final Box optionsBox = horizontalBox(
						new JLabel("Q:"), qField,
						new JLabel("S:"), sField);
				final Image2DComponent imageComponent = new Image2DComponent(new AwtImage2D("", 256, 256)).setDropImageEnabled(true);
				final Collection<String> keys = new HashSet<>();
				final AtomicInteger hash = new AtomicInteger(1);
				
				mainPanel.add(optionsBox, BorderLayout.NORTH);
				mainPanel.add(imageComponent, BorderLayout.CENTER);
				
				final ActionListener fieldActionListener = new ActionListener() {
					
					@Override
					public final void actionPerformed(final ActionEvent event) {
						keys.forEach(IMJCoreTools::uncache);
						keys.clear();
						hash.incrementAndGet();
						imageComponent.repaint();
					}
					
				};
				
				qField.addActionListener(fieldActionListener);
				sField.addActionListener(fieldActionListener);
				
				final ExecutorService executor = Executors.newFixedThreadPool(2);
				
				imageComponent.setTileOverlay(new TileOverlay() {
					
					@Override
					public final int hashCode() {
						return hash.get();
					}
					
					@Override
					public final void update(final Graphics2D graphics, final Point tileXY, final Rectangle region) {
						final Image2D image = imageComponent.getImage();
						final String key = image.getTileKey(tileXY.x, tileXY.y) + "_outlines";
						final Reference<Image2D> cached = IMJCoreTools.getCached(key);
						
						if (cached == null) {
							graphics.setColor(Color.RED);
							graphics.draw(region);
							
							if (keys.add(key)) {
								executor.submit(new Runnable() {
									
									@Override
									public final void run() {
										cache(key, new Supplier<Image2D>() {
											
											@Override
											public final Image2D get() {
												final Monitor monitor = new Monitor() {
													
													@Override
													public boolean isCancelRequested() {
														return (!keys.contains(key));
													}
													
													private static final long serialVersionUID = -3736045057872099915L;
													
												};
												
												final int q = Integer.decode(qField.getText());
												final int s = Integer.decode(sField.getText());
												final Image2D tile = image.getTile(tileXY.x, tileXY.y);
												final int w = tile.getWidth();
												final int h = tile.getHeight();
												final BufferedImage segments = segment(tile, q, s, 0, monitor);
												
												if (segments == null) {
													return null;
												}
												
												final AwtImage2D result = new AwtImage2D("", w, h);
												
												for (int y = 0; y < h; ++y) {
													if (monitor.isCancelRequested()) {
														return null;
													}
													
													for (int x = 0; x < w; ++x) {
														final long center = segments.getRGB(x, y);
														boolean mark = false;
														
														if (0 < y) {
															mark = mark || center != segments.getRGB(x, y - 1);
														}
														
														if (0 < x) {
															mark = mark || center != segments.getRGB(x - 1, y);
														}
														
														mark = mark || x == 0 || x == w - 1 || y == 0 || y == h - 1;
														
														if (mark) {
															result.setPixelValue(x, y, 0xFF00FF00L);
														}
													}
												}
												
												return result;
											}
											
										});
										
										hash.incrementAndGet();
										imageComponent.repaint();
									}
									
								});
							}
						} else if (cached.hasObject()) {
							graphics.drawImage((Image) cached.getObject().toAwt(), region.x, region.y, region.width, region.height, null);
						} else {
							graphics.setColor(Color.RED);
							graphics.draw(region);
						}
					}
					
					private static final long serialVersionUID = -4573576871706331668L;
					
				});
				
				imageComponent.setOverlay(new Overlay() {
					
					private final Canvas canvas = new Canvas();
					
					@Override
					public final void update(final Graphics2D graphics, final Rectangle region) {
						if (region.isEmpty()) {
							return;
						}
						
						this.canvas.setFormat(region.width, region.height).clear(new Color(0, true));
						
						graphics.setColor(Color.BLUE);
						graphics.draw(region);
						
						final Point2D p0 = new Point2D.Double(0.0, 0.0);
						final Point2D p1 = new Point2D.Double(1.0, 1.0);
						
						try {
							imageComponent.getView().inverseTransform(p0, p0);
							imageComponent.getView().inverseTransform(p1, p1);
							
							final double dx = p1.getX() - p0.getX();
							final double dy = p1.getY() - p0.getY();
							final int w = region.width;
							final int h = region.height;
							final Image2D image = imageComponent.getImage();
							final double scale = image.getScale();
							
							for (int y = 0; y < h; ++y) {
								for (int x = 0; x < w; ++x) {
									final int xIm = (int) ((p0.getX() + x * dx) * scale);
									final int yIm = (int) ((p0.getY() + y * dy) * scale);
									final int north = (int) ((p0.getY() + (y - 1) * dy) * scale);
									final int west = (int) ((p0.getX() + (x - 1) * dx) * scale);
									
									if (0 <= xIm && xIm < w && 0 <= yIm && yIm < h) {
										boolean mark = false;
										
										if (0 <= north && north < y) {
											// TODO
										}
										
										if (0 <= west && west < x) {
											// TODO
										}
										
										if (mark) {
											this.canvas.getImage().setRGB(x, y, 0xFF00FF00);
										}
									}
								}
							}
							
							graphics.drawImage(this.canvas.getImage(), 0, 0, null);
						} catch (final NoninvertibleTransformException exception) {
							exception.printStackTrace();
						}
					}
					
					private static final long serialVersionUID = -6245612608729813604L;
					
				});
				
				final Window mainFrame = show(mainPanel, "Image");
				
				mainFrame.addWindowListener(new WindowAdapter() {
					
					@Override
					public final void windowClosing(final WindowEvent event) {
						executor.shutdown();
						keys.clear();
					}
					
				});
			}
			
		});
	}
	
	public static final void segmentFull(final Image2D image, final int q, final int s, final String outputPath, final Monitor monitor) {
		try (final ZipOutputStream output = new ZipOutputStream(new FileOutputStream(outputPath))) {
			final int w0 = image.getWidth();
			final int h0 = image.getHeight();
			final String tileFormat = "png";
			final Document xml = newMetadata(w0, h0,
					image.getOptimalTileWidth(), image.getOptimalTileHeight(),
					tileFormat, image.getMetadata().get("micronsPerPixel").toString(), new int[] { 0 });
			
			output.putNextEntry(new ZipEntry("metadata.xml"));
			XMLTools.write(xml, output, 0);
			output.closeEntry();
			
			final List<Node> nodes = XMLTools.getNodes(xml, "//image");
			
			for (final Node imageNode : nodes) {
				final String type = XMLTools.getString(imageNode, "@type");
				final int w = XMLTools.getNumber(imageNode, "@width").intValue();
				final int h = XMLTools.getNumber(imageNode, "@height").intValue();
				final Image2D im = image.getScaledImage(max((double) w / w0, (double) h / h0));
				
				debugPrint(type, w, h, im.getWidth(), im.getHeight());
				
				forEachTile(w, h, im.getOptimalTileWidth(), im.getOptimalTileHeight(), new Pixel2DProcessor() {
					
					@Override
					public final boolean pixel(final int tileX, final int tileY) {
						final BufferedImage segments = segment(im.getTile(tileX, tileY), q, s, 0xFF000000, monitor);
						
						debugPrint(type, tileX, tileY);
						
						try {
							output.putNextEntry(new ZipEntry("tiles/tile_" + type + "_y" + tileY + "_x" + tileX + "." + tileFormat));
							ImageIO.write(segments, tileFormat, output);
							output.closeEntry();
						} catch (final IOException exception) {
							throw new UncheckedIOException(exception);
						}
						
						return segments != null;
					}
					
					private static final long serialVersionUID = 2620298775908199381L;
					
				});
			}
			
			SVS2Multifile.includeHTMLViewer(output, w0, h0, image.getOptimalTileWidth(), nodes.size() - 1, tileFormat);
		} catch (final IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}
	
	public static final BufferedImage segment(final Image2D image, final int q, final int s, final int id0, final Monitor monitor) {
		final int w = image.getWidth();
		final int h = image.getHeight();
		final BufferedImage transformedMiddle = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		final BufferedImage segments = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		final Channels channels = image.getChannels();
		final int qMask = ((~0) << q) & 0xFF;
		
		for (int y = 0; y < h; ++y) {
			if (monitor.isCancelRequested()) {
				return null;
			}
			
			for (int x = 0; x < w; ++x) {
				final long value = image.getPixelValue(x, y);
				final int r = (int) channels.getChannelValue(value, 0);
				final int g = (int) channels.getChannelValue(value, 1);
				final int b = (int) channels.getChannelValue(value, 2);
				long newMiddleValue = channels.setChannelValue(0L, 0, qMask & r);
				newMiddleValue = channels.setChannelValue(newMiddleValue, 1, qMask & g);
				newMiddleValue = channels.setChannelValue(newMiddleValue, 2, qMask & b);
				
				transformedMiddle.setRGB(x, y, (int) newMiddleValue);
			}
		}
		
		final int[] id = { id0 };
		
		forEachTile(w, h, s, s, new Pixel2DProcessor() {
			
			@Override
			public final boolean pixel(final int tileX, final int tileY) {
				if (monitor.isCancelRequested()) {
					id[0] = -1;
					return false;
				}
				
				final int tw = min(s, w - tileX);
				final int th = min(s, h - tileY);
				
				forEachPixelInEachComponent4(image, new Rectangle(tileX, tileY, tw, th), new ComponentComembership() {
					
					@Override
					public final boolean test(final Image2D image, final long pixel, final long otherPixel) {
						final int x = image.getX(pixel);
						final int y = image.getY(pixel);
						final int otherX = image.getX(otherPixel);
						final int otherY = image.getY(otherPixel);
						
						return transformedMiddle.getRGB(x, y) == transformedMiddle.getRGB(otherX, otherY);
					}
					
					private static final long serialVersionUID = 6632151904983956288L;
					
				}, new Pixel2DProcessor() {
					
					@Override
					public final boolean pixel(final int x, final int y) {
						segments.setRGB(x, y, id[0]);
						
						return true;
					}
					
					@Override
					public final void endOfPatch() {
						++id[0];
					}
					
					private static final long serialVersionUID = -6435721813416466409L;
					
				});
				
				return true;
			}
			
			private static final long serialVersionUID = 13671048674063813L;
			
		});
		
		return id[0] == -1 ? null : segments;
	}
	
	/**
	 * @author codistmonk (creation 2016-06-06)
	 */
	public static abstract interface Monitor extends Serializable {
		
		public abstract boolean isCancelRequested();
		
		public static final Monitor DEFAULT = () -> false;
		
	}
	
}
