package imj2.tools;

import static java.awt.Color.BLUE;
import static java.awt.Color.CYAN;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static java.awt.Color.YELLOW;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import imj2.tools.Image2DComponent.Painter;
import imj2.tools.RegionShrinkingTest.AutoMouseAdapter;
import imj2.tools.RegionShrinkingTest.SimpleImageView;

import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-02-07)
 */
public final class MultiresolutionSegmentationTest {
	
	@Test
	public final void test() {
		final SimpleImageView imageView = new SimpleImageView();
		
		new AutoMouseAdapter(imageView.getImageHolder()) {
			
			private BufferedImage[] pyramid;
			
			private int cellSize = 4;
			
			private final Painter<SimpleImageView> painter = new Painter<SimpleImageView>() {
				
				private Point[] particles;
				
				{
					imageView.getPainters().add(this);
				}
				
				@Override
				public final void paint(final Graphics2D g, final SimpleImageView component,
						final int width, final int height) {
					refreshLODs();
					
					final BufferedImage[] pyramid = getPyramid();
					final int lodCount = pyramid.length;
					final int s0 = getCellSize();
					final int widthLOD0 = pyramid[0].getWidth();
					final int heightLOD0 = pyramid[0].getHeight();
					final boolean debugGradient = false;
					
					if (debugGradient && s0 < lodCount) {
						final int lod = s0;
						debugPrint(lod);
						final int dimensionMask = (-1) << lod;
						final int w = widthLOD0 & dimensionMask;
						final int h = heightLOD0 & dimensionMask;
						final BufferedImage image = pyramid[lod];
						
						for (int y = 0; y < h; ++y) {
							for (int x = 0; x < w; ++x) {
								try {
									component.getBuffer().setRGB(x, y, gray888(getColorGradient(image, x >> lod, y >> lod)));
								} catch (final Exception exception) {
									debugPrint(x, y, x >> lod, y >> lod, image.getWidth(), image.getHeight());
									throw unchecked(exception);
								}
							}
						}
					}
					
					if (0 < s0) {
						for (int lod = lodCount - 1; 0 <= lod; --lod) {
							final BufferedImage image = pyramid[lod];
							final int w = image.getWidth();
							final int h = image.getHeight();
							final int diameter = 1 + 2 * lod;
							
							for (int particleY = s0; particleY < h; particleY += s0) {
								for (int particleX = s0; particleX < w; particleX += s0) {
									g.setColor(Color.RED);
									g.drawOval((particleX << lod) - lod, (particleY << lod) - lod, diameter, diameter);
								}
							}
						}
					}
//					final int horizontalParticleCount = widthLOD0 / s0;
//					final int verticalParticleCount = heightLOD0 / s0;
//					
//					debugPrint(horizontalParticleCount, verticalParticleCount);
//					
//					this.particles = new Point[horizontalParticleCount * verticalParticleCount];
//					final Color[] particleColors = new Color[this.particles.length];
//					final Color colors[] = { RED, GREEN, BLUE, YELLOW, CYAN };
//					
//					for (int i = 0; i < verticalParticleCount; ++i) {
//						for (int j = 0; j < horizontalParticleCount; ++j) {
//							this.particles[i * horizontalParticleCount + j] = new Point(j * s0, i * s0);
//						}
//					}
//					
//					for (int lod = pyramid.length - 1; 0 <= lod; --lod) {
//						final int s = s0 << lod;
//						final BufferedImage image = pyramid[lod];
//						final int w = image.getWidth();
//						final int h = image.getHeight();
//						
//						if (s < w && s < h) {
//							final int d = 1 << lod;
//							
//							for (int i = d; i < verticalParticleCount; i += d) {
//								for (int j = d; j < horizontalParticleCount; j += d) {
//									if (((i / d) & 1) == 1 || ((j / d) & 1) == 1) {
//										final int particleIndex = i * horizontalParticleCount + j;
//										final Point particle = this.particles[particleIndex];
//										particleColors[particleIndex] = colors[lod];
//									}
//								}
//							}
//						}
//					}
//					
//					if (true) {
//						for (int particleIndex = 0; particleIndex < this.particles.length; ++particleIndex) {
//							final Point particle = this.particles[particleIndex];
//							
//							g.setColor(particleColors[particleIndex]);
//							g.drawOval(particle.x - 1, particle.y - 1, 3, 3);
//						}
//					} else {
//						g.setColor(RED);
//						
//						for (final Point particle : this.particles) {
//							g.drawOval(particle.x - 1, particle.y - 1, 3, 3);
//						}
//					}
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = -8692596860025480748L;
				
			};
			
			@Override
			public final void mouseWheelMoved(final MouseWheelEvent event) {
				if (event.getWheelRotation() < 0 && 0 < this.getCellSize()) {
					--this.cellSize;
					imageView.refreshBuffer();
				}
				
				if (0 < event.getWheelRotation()) {
					++this.cellSize;
					imageView.refreshBuffer();
				}
			}
			
			public final boolean refreshLODs() {
				if (this.getPyramid() == null || this.getPyramid().length == 0 || this.getPyramid()[0] != imageView.getImage()) {
					this.pyramid = makePyramid(imageView.getImage());
					
					return true;
				}
				
				return false;
			}
			
			public final BufferedImage[] getPyramid() {
				return this.pyramid;
			}
			
			public final int getCellSize() {
				return this.cellSize;
			}
			
			@Override
			protected final void cleanup() {
				imageView.getPainters().remove(this.painter);
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = 3954986726959359787L;
			
		};
		
		show(imageView, "Simple Image View", true);
	}
	
	public static final int MAXIMUM_COLOR_DISTANCE = getColorDistance(0x00000000, 0x00FFFFFF);
	
	public static final int gray888(final int value8) {
		return value8 | (value8 << 8) | (value8 << 16);
	}
	
	public static final int getColorGradient(final BufferedImage image, final int x, final int y) {
		final int maxX = image.getWidth() - 1;
		final int maxY = image.getHeight() - 1;
		final int rgb = image.getRGB(x, y);
		int result = 0;
		
		if (0 < x) {
			if (0 < y) {
				result = max(result, getColorDistance(rgb, image.getRGB(x - 1, y - 1)));
			}
			
			if (y < maxY) {
				result = max(result, getColorDistance(rgb, image.getRGB(x - 1, y + 1)));
			}
		}
		
		if (x < maxX) {
			if (0 < y) {
				result = max(result, getColorDistance(rgb, image.getRGB(x + 1, y - 1)));
			}
			
			if (y < maxY) {
				result = max(result, getColorDistance(rgb, image.getRGB(x + 1, y + 1)));
			}
		}
		
		return result * 255 / MAXIMUM_COLOR_DISTANCE;
	}
	
	public static final int getColorDistance(final int rgb1, final int rgb2) {
		return abs(red8(rgb1) - red8(rgb2)) + abs(green8(rgb1) - green8(rgb2)) + abs(blue8(rgb1) - blue8(rgb2));
	}
	
	public static final BufferedImage[] makePyramid(final BufferedImage lod0) {
		final List<BufferedImage> lods = new ArrayList<BufferedImage>();
		BufferedImage lod = lod0;
		int w = lod.getWidth();
		int h = lod.getHeight();
		
		do {
			lods.add(lod);
			
			final BufferedImage nextLOD = new BufferedImage(w /= 2, h /= 2, lod.getType());
			
			for (int y = 0; y < h; ++y) {
				for (int x = 0; x < w; ++x) {
					final int c00 = lod.getRGB(2 * x + 0, 2 * y + 0);
					final int c10 = lod.getRGB(2 * x + 1, 2 * y + 0);
					final int c01 = lod.getRGB(2 * x + 0, 2 * y + 1);
					final int c11 = lod.getRGB(2 * x + 1, 2 * y + 1);
					
					nextLOD.setRGB(x, y,
							mean(red(c00), red(c10), red(c01), red(c11)) |
							mean(green(c00), green(c10), green(c01), green(c11)) |
							mean(blue(c00), blue(c10), blue(c01), blue(c11)));
				}
			}
			
			lod = nextLOD;
		} while (1 < w && 1 < h);
		
		return lods.toArray(new BufferedImage[0]);
	}
	
	public static final int red8(final int rgb) {
		return red(rgb) >> 16;
	}
	
	public static final int green8(final int rgb) {
		return green(rgb) >> 8;
	}
	
	public static final int blue8(final int rgb) {
		return blue(rgb);
	}
	
	public static final int red(final int rgb) {
		return rgb & 0x00FF0000;
	}
	
	public static final int green(final int rgb) {
		return rgb & 0x0000FF00;
	}
	
	public static final int blue(final int rgb) {
		return rgb & 0x000000FF;
	}
	
	public static final int mean(final int... values) {
		int sum = 0;
		
		for (final int value : values) {
			sum += value;
		}
		
		return 0 < values.length ? sum / values.length : 0;
	}
	
}
