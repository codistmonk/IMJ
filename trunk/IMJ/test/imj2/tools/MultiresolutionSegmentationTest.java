package imj2.tools;

import static java.awt.Color.RED;
import static java.awt.Color.YELLOW;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.instances;
import static net.sourceforge.aprog.tools.Tools.unchecked;
import imj2.tools.Image2DComponent.Painter;
import imj2.tools.RegionShrinkingTest.AutoMouseAdapter;
import imj2.tools.RegionShrinkingTest.SimpleImageView;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.aprog.tools.Factory.DefaultFactory;

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
			
			private int cellSize = 8;
			
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
						int startingLOD = -1;
						
						for (int lod = lodCount - 1; 0 <= lod; --lod) {
							final BufferedImage image = pyramid[lod];
							final int w = image.getWidth();
							final int h = image.getHeight();
							final int diameter = 1 + 2 * lod;
							
							for (int particleY = s0; particleY < h; particleY += s0) {
								for (int particleX = s0; particleX < w; particleX += s0) {
									if (startingLOD < 0) {
										startingLOD = lod;
									}
									
									g.setColor(RED);
									g.drawOval((particleX << lod) - lod, (particleY << lod) - lod, diameter, diameter);
								}
							}
						}
						
						{
							debugPrint(startingLOD);
							
							Grid grid = null;
							
							for (int lod = startingLOD; 0 <= lod; --lod) {
								grid = grid == null ? new Grid(pyramid[lod], s0) : grid.refine(pyramid[lod]);
							}
							
							if (grid != null) {
								for (final Point vertex : grid.getVertices()) {
									g.setColor(YELLOW);
									g.drawOval(vertex.x - 1, vertex.y - 1, 3, 3);
								}
							}
						}
					}
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
	
	/**
	 * @author codistmonk (creation 2014-02-21)
	 */
	public static final class Grid implements Serializable {
		
		private final int cellSize;
		
		private final int horizontalVertexCount;
		
		private final int verticalVertexCount;
		
		private final Point[] vertices;
		
		public Grid(final BufferedImage image, final int cellSize) {
			this(cellSize, 2 + (image.getWidth() - 1) / cellSize, 2 + (image.getHeight() - 1) / cellSize);
			
			final int width = image.getWidth();
			final int height = image.getHeight();
			
			for (int i = 0, vertexIndex = 0; i < this.verticalVertexCount; ++i) {
				for (int j = 0; j < this.horizontalVertexCount; ++j, ++vertexIndex) {
					this.vertices[vertexIndex].setLocation(
							j + 1 == this.horizontalVertexCount ? width - 1 : cellSize * j,
							i + 1 == this.verticalVertexCount ? height - 1 : cellSize * i);
				}
			}
		}
		
		private Grid(final int cellSize, final int horizontalVertexCount, final int verticalVertexCount) {
			this.cellSize = cellSize;
			this.horizontalVertexCount = horizontalVertexCount;
			this.verticalVertexCount = verticalVertexCount;
			this.vertices = instances(this.horizontalVertexCount * this.verticalVertexCount,
					DefaultFactory.forClass(Point.class));
		}
		
		public final int getHorizontalVertexCount() {
			return this.horizontalVertexCount;
		}
		
		public final int getVerticalVertexCount() {
			return this.verticalVertexCount;
		}
		
		public final Point[] getVertices() {
			return this.vertices;
		}
		
		public final Point getVertex(final int verticalIndex, final int horizontalIndex) {
			return this.getVertices()[verticalIndex * this.getHorizontalVertexCount() + horizontalIndex];
		}
		
		public final Grid refine(final BufferedImage image) {
			final Grid result = new Grid(this.cellSize,
					this.getHorizontalVertexCount() * 2 - 1, this.getVerticalVertexCount() * 2 - 1);
			final int width = image.getWidth();
			final int height = image.getHeight();
			
			for (int i = 0; i < this.getVerticalVertexCount(); ++i) {
				final int ii = i * 2;
				
				for (int j = 0; j < this.getHorizontalVertexCount(); ++j) {
					final int jj = j * 2;
					final Point thisVertex = this.getVertex(i, j);
					final Point thatVertex = result.getVertex(ii, jj);
					
					thatVertex.setLocation(
							j + 1 == this.getHorizontalVertexCount() ? width - 1 : thisVertex.x * 2,
							i + 1 == this.getVerticalVertexCount() ? height - 1 : thisVertex.y * 2);
					
					final Point westVertex = 0 < j ? result.getVertex(ii, jj - 2) : null;
					final Point northVertex = 0 < i ? result.getVertex(ii - 2, jj) : null;
					final Point northWestVertex = northVertex != null && westVertex != null ? result.getVertex(ii - 2, jj - 2) : null;
					
					if (westVertex != null) {
						result.getVertex(ii, jj - 1).setLocation(
								(thatVertex.x + westVertex.x) / 2, (thatVertex.y + westVertex.y) / 2);
					}
					
					if (northVertex != null) {
						result.getVertex(ii - 1, jj).setLocation(
								(thatVertex.x + northVertex.x) / 2, (thatVertex.y + northVertex.y) / 2);
					}
					
					if (northWestVertex != null) {
						result.getVertex(ii - 1, jj - 1).setLocation(
								(thatVertex.x + westVertex.x + northVertex.x + northWestVertex.x) / 4,
								(thatVertex.y + westVertex.y + northVertex.y + northWestVertex.y) / 4);
					}
				}
			}
			
			if (true) {
				return result;
			}
			
			for (int i = 0, vertexIndex = 0; i < result.getVerticalVertexCount(); ++i) {
				for (int j = 0; j < result.getHorizontalVertexCount(); ++j, ++vertexIndex) {
//					if (i == 0) {
//						result.getVertices()[vertexIndex].setLocation(j + 1 == result.getHorizontalVertexCount() ? width - 1 : this.cellSize * j, 0);
//					} else if (j == 0) {
//						result.getVertices()[vertexIndex].setLocation(0, i + 1 == result.getVerticalVertexCount() ? height - 1 : this.cellSize * i);
//					} else if (j + 1 == result.getHorizontalVertexCount() - 1) {
//						result.getVertices()[vertexIndex].setLocation(width - 1, i + 1 == result.getVerticalVertexCount() ? height - 1 : this.cellSize * i);
//					} else if (i + 1 == result.getVerticalVertexCount() - 1) {
//						result.getVertices()[vertexIndex].setLocation(j + 1 == result.getHorizontalVertexCount() ? width - 1 : this.cellSize * j, height - 1);
//					}
					final int x;
					final int y;
					
					if (i == 0) {
						y = 0;
						
						if (j == 0) {
							x = 0;
						} else if (j + 1 == result.getVerticalVertexCount()) {
							x = width - 1;
						} else if (isEven(j)) {
							x = this.getVertices()[0 * this.getHorizontalVertexCount() + j / 2].x * 2;
						} else {
							x = this.getVertices()[0 * this.getHorizontalVertexCount() + (j - 1) / 2].x +
									this.getVertices()[0 * this.getHorizontalVertexCount() + (j + 1) / 2].x;
						}
					} else if (i + 1 == result.getVerticalVertexCount()) {
						y = height - 1;
//					} else if (isEven(i)) {
//						y = this.getVertices()[(i / 2) * this.getHorizontalVertexCount() + j / 2].y * 2;
					}
					
					
					if (i != 0 && i + 1 != result.getVerticalVertexCount() && isEven(i) && j != 0 && j + 1 != result.getHorizontalVertexCount() && isEven(j)) {
						final Point thisVertex = this.getVertices()[(i / 2) * this.getHorizontalVertexCount() + j / 2];
						
						result.getVertices()[vertexIndex].setLocation(thisVertex.x * 2, thisVertex.y * 2);
					} else {
						result.getVertices()[vertexIndex].setLocation(
								j + 1 == result.getHorizontalVertexCount() ? width - 1 : this.cellSize * j,
										i + 1 == result.getVerticalVertexCount() ? height - 1 : this.cellSize * i);
					}
				}
			}
			
			return result;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -6546839284908468675L;
		
		public static final boolean isEven(final int n) {
			return (n & 1) == 0;
		}
		
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
