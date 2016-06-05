package imj3.draft;

import static imj3.core.IMJCoreTools.cache;
import static imj3.tools.IMJTools.forEachPixelInEachComponent4;
import static imj3.tools.IMJTools.forEachTile;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static multij.swing.SwingTools.horizontalBox;
import static multij.swing.SwingTools.show;

import imj3.core.Channels;
import imj3.core.IMJCoreTools;
import imj3.core.IMJCoreTools.Reference;
import imj3.core.Image2D;
import imj3.core.Image2D.Pixel2DProcessor;
import imj3.tools.AwtImage2D;
import imj3.tools.IMJTools.ComponentComembership;
import imj3.tools.Image2DComponent;
import imj3.tools.Image2DComponent.TileOverlay;

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
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2016-06-03)
 */
public final class QuickSeg {
	
	private QuickSeg() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Must not be null
	 */
	public static final void main(final String... commandLineArguments) {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public final void run() {
				final JPanel mainPanel = new JPanel(new BorderLayout());
				final JTextField gammaField = new JTextField("1.0");
				final JTextField qField = new JTextField("6");
				final JTextField sField = new JTextField("32");
				final Box optionsBox = horizontalBox(
						new JLabel("G:"), gammaField,
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
				
				gammaField.addActionListener(fieldActionListener);
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
												final double gamma = Double.parseDouble(gammaField.getText());
												final int q = Integer.decode(qField.getText());
												final int s = Integer.decode(sField.getText());
												final Image2D tile = image.getTile(tileXY.x, tileXY.y);
												final int w = tile.getWidth();
												final int h = tile.getHeight();
												final BufferedImage transformedLow = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
												final BufferedImage transformedMiddle = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
												final BufferedImage transformedHigh = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
												final BufferedImage segments = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
												final Channels channels = tile.getChannels();
												final int qMask = ((~0) << q) & 0xFF;
												
												for (int y = 0; y < h; ++y) {
													if (!keys.contains(key)) {
														return null;
													}
													for (int x = 0; x < w; ++x) {
														final long value = tile.getPixelValue(x, y);
														
														final double middleR = channels.getChannelValue(value, 0) / 255.0;
														final double lowR = pow(middleR, 1.0 / gamma);
														final double highR = pow(middleR, gamma);
														final double middleG = channels.getChannelValue(value, 1) / 255.0;
														final double lowG = pow(middleG, 1.0 / gamma);
														final double highG = pow(middleG, gamma);
														final double middleB = channels.getChannelValue(value, 2) / 255.0;
														final double lowB = pow(middleB, 1.0 / gamma);
														final double highB = pow(middleB, gamma);
														
														long newLowValue = channels.setChannelValue(0L, 0, qMask & (int) (lowR * 255.0));
														newLowValue = channels.setChannelValue(newLowValue, 1, qMask & (int) (lowG * 255.0));
														newLowValue = channels.setChannelValue(newLowValue, 2, qMask & (int) (lowB * 255.0));
														long newMiddleValue = channels.setChannelValue(0L, 0, qMask & (int) (middleR * 255.0));
														newMiddleValue = channels.setChannelValue(newMiddleValue, 1, qMask & (int) (middleG * 255.0));
														newMiddleValue = channels.setChannelValue(newMiddleValue, 2, qMask & (int) (middleB * 255.0));
														long newHighValue = channels.setChannelValue(0L, 0, qMask & (int) (highR * 255.0));
														newHighValue = channels.setChannelValue(newHighValue, 1, qMask & (int) (highG * 255.0));
														newHighValue = channels.setChannelValue(newHighValue, 2, qMask & (int) (highB * 255.0));
														
														transformedLow.setRGB(x, y, (int) newLowValue);
														transformedMiddle.setRGB(x, y, (int) newMiddleValue);
														transformedHigh.setRGB(x, y, (int) newHighValue);
													}
												}
												
												final int[] id = { 0 };
												
												forEachTile(w, h, s, s, new Pixel2DProcessor() {
													
													@Override
													public final boolean pixel(final int tileX, final int tileY) {
														if (!keys.contains(key)) {
															return false;
														}
														
														final int tw = min(s, w - tileX);
														final int th = min(s, h - tileY);
														
														forEachPixelInEachComponent4(tile, new Rectangle(tileX, tileY, tw, th), new ComponentComembership() {
															
															@Override
															public final boolean test(final Image2D image, final long pixel, final long otherPixel) {
																final int x = image.getX(pixel);
																final int y = image.getY(pixel);
																final int otherX = image.getX(otherPixel);
																final int otherY = image.getY(otherPixel);
																
																return transformedLow.getRGB(x, y) == transformedLow.getRGB(otherX, otherY)
																		&& transformedMiddle.getRGB(x, y) == transformedMiddle.getRGB(otherX, otherY)
																		&& transformedHigh.getRGB(x, y) == transformedHigh.getRGB(otherX, otherY);
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
												
												final AwtImage2D result = new AwtImage2D("", w, h);
												
												for (int y = 0; y < h; ++y) {
													if (!keys.contains(key)) {
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
}
