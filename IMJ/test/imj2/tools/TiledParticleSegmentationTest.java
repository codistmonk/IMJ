package imj2.tools;

import static imj2.tools.MultiresolutionSegmentationTest.getColorGradient;
import static imj2.tools.TiledParticleSegmentationTest.XYW.xyw;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.sort;
import static java.util.Collections.nCopies;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getOrCreate;

import imj.IntList;

import imj2.tools.Image2DComponent.Painter;
import imj2.tools.RegionShrinkingTest.AutoMouseAdapter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.tools.TicToc;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-02-23)
 */
public final class TiledParticleSegmentationTest {
	
	/**
	 * {@value}.
	 */
	public static final int NORTH = 0;
	
	/**
	 * {@value}.
	 */
	public static final int WEST = 1;
	
	/**
	 * {@value}.
	 */
	public static final int EAST = 2;
	
	/**
	 * {@value}.
	 */
	public static final int SOUTH = 3;
	
	@Test
	public final void test() {
		final int quantizationAlgorithm = 1;
		final boolean fillSegments = false;
		final Color regionSeparationColor = Color.GREEN;
		final Color segmentSeparationColor = null;
		final Color segmentLocatorColor = null;
		final int algo0Q = 6;
		final int algo1Q = 3;
		
		final SimpleImageView imageView = new SimpleImageView();
		
		new AutoMouseAdapter(imageView.getImageHolder()) {
			
			private int cellSize = 7;
			
			private final Painter<SimpleImageView> painter = new Painter<SimpleImageView>() {
				
				private final Canvas segmentation;
				
				{
					this.segmentation = new Canvas();
					imageView.getPainters().add(this);
				}
				
				@Override
				public final void paint(final Graphics2D g, final SimpleImageView component,
						final int width, final int height) {
					final TicToc timer = new TicToc();
					final BufferedImage image = imageView.getImage();
					final int imageWidth = image.getWidth();
					final int imageHeight = image.getHeight();
					
					this.segmentation.setFormat(imageWidth, imageHeight, BufferedImage.TYPE_BYTE_GRAY);
					this.segmentation.clear(BLACK);
					
					final int s = getCellSize();
					final int horizontalTileCount = (imageWidth + s - 1) / s + 1;
					final int verticalTileCount = (imageHeight + s - 1) / s + 1;
					
					final int[] northXs = new int[horizontalTileCount * verticalTileCount];
					final int[] westYs = new int[horizontalTileCount * verticalTileCount];
					
					debugPrint("Segmenting...", new Date(timer.tic()));
					
					for (int tileY = 0; tileY + 2 < imageHeight; tileY += s) {
						final int tileLastY = imageHeight <= tileY + s + 2 ? imageHeight - 1 : min(imageHeight - 1, tileY + s);
						
						for (int tileX = 0; tileX + 2 < imageWidth; tileX += s) {
							final int tileLastX = imageWidth <= tileX + s + 2 ? imageWidth - 1 : min(imageWidth - 1, tileX + s);
							final int northY = tileY;
							final int westX = tileX;
							final int eastX = tileLastX;
							final int southY = tileLastY;
							final int northX = findMaximumGradientX(image, northY, westX + 1, eastX - 1);
							final int westY = findMaximumGradientY(image, westX, northY + 1, southY - 1);
							final int eastY = findMaximumGradientY(image, eastX, northY + 1, southY - 1);
							final int southX = findMaximumGradientX(image, southY, westX + 1, eastX - 1);
							final int tileIndex = tileY / s * horizontalTileCount + tileX / s;
							northXs[tileIndex] = northX;
							northXs[tileIndex + horizontalTileCount] = southX;
							westYs[tileIndex] = westY;
							westYs[tileIndex + 1] = eastY;
							
							this.segmentation.getGraphics().setColor(WHITE);
							this.segmentation.getGraphics().drawLine(northX, northY, southX, southY);
							this.segmentation.getGraphics().drawLine(westX, westY, eastX, eastY);
						}
					}
					
					debugPrint("Segmenting done in", timer.toc(), "ms");
					
					final BufferedImage segmentationMask = this.segmentation.getImage();
					
					if (segmentSeparationColor != null) {
						debugPrint("Filling segments started", new Date(timer.tic()));
						
						for (int y = 0; y < imageHeight; ++y) {
							for (int x = 0; x < imageWidth; ++x) {
								if ((segmentationMask.getRGB(x, y) & 0x00FFFFFF) != 0) {
									imageView.getBufferImage().setRGB(x, y, segmentSeparationColor.getRGB());
								}
							}
						}
						
						debugPrint("Filling segments done in", timer.toc(), "ms");
					}
					
					{
						debugPrint("Computing descriptors...", new Date(timer.tic()));
						
						final List<Color> colors = new ArrayList<Color>(nCopies(horizontalTileCount * verticalTileCount, Color.BLACK));
						
						for (int tileY = 0; tileY < imageHeight; tileY += s) {
							for (int tileX = 0; tileX < imageWidth; tileX += s) {
								final Color[] color = { Color.YELLOW };
								
								new ComputeMeanColor(image, color).process(segmentationMask, tileX, tileY);
								
								colors.set(tileY / s * horizontalTileCount + tileX / s , color[0]);
							}
						}
						
						debugPrint("Computing descriptors done in", timer.toc(), "ms");
						debugPrint("tileCount:", colors.size(), "horizontalTileCount:", horizontalTileCount, "verticalTileCount:", verticalTileCount);
						
						{
							final Color[] sortedColors = new HashSet<Color>(colors).toArray(new Color[0]);
							final int colorCount = sortedColors.length;
							final int[] redPrototypes = new int[min(algo1Q, colorCount)];
							final int[] greenPrototypes = redPrototypes.clone();
							final int[] bluePrototypes = redPrototypes.clone();
							
							if (quantizationAlgorithm == 1) {
								debugPrint("Clustering descriptors...", new Date(timer.tic()));
								
								if (algo1Q < colorCount) {
									sort(sortedColors, COLOR_RED_COMPARATOR);
									computePrototypes(sortedColors, 16, algo1Q, redPrototypes);
									sort(sortedColors, COLOR_GREEN_COMPARATOR);
									computePrototypes(sortedColors, 8, algo1Q, greenPrototypes);
									sort(sortedColors, COLOR_BLUE_COMPARATOR);
									computePrototypes(sortedColors, 8, algo1Q, bluePrototypes);
								}
								
								debugPrint(Arrays.toString(redPrototypes));
								debugPrint(Arrays.toString(greenPrototypes));
								debugPrint(Arrays.toString(bluePrototypes));
								
								debugPrint("Clustering descriptors done in", timer.toc(), "ms");
							}
							
							debugPrint("Quantizing descriptors...", new Date(timer.tic()));
							
							final List<Color> quantizedColors = new ArrayList<Color>(colorCount);
							final Set<Color> uniqueQuantizedColors = new HashSet<Color>();
							
							for (final Color color : colors) {
								final Color quantizedColor;
								
								if (quantizationAlgorithm == 0) {
									quantizedColor = quantize(color, algo0Q);
								} else {
									quantizedColor = quantize(color, redPrototypes, greenPrototypes, bluePrototypes);
								}
								
								quantizedColors.add(quantizedColor);
								uniqueQuantizedColors.add(quantizedColor);
							}
							
							debugPrint("Quantizing descriptors done in", timer.toc(), "ms");
							
							debugPrint("uniqueQuantizedColorCount:", uniqueQuantizedColors.size(), "uniqueQuantizedColors:", uniqueQuantizedColors);
							
							debugPrint("Labeling...", new Date(timer.tic()));
							
							final int[] labels = new int[imageWidth * imageHeight];
							
							for (int tileY = s, tileRowIndex = tileY / s; tileY < imageHeight; tileY += s, ++tileRowIndex) {
								for (int tileX = s, tileColumnIndex = tileX / s; tileX < imageWidth; tileX += s, ++tileColumnIndex) {
									final int tileIndex = tileRowIndex * horizontalTileCount + tileColumnIndex;
									final int rgb = quantizedColors.get(tileIndex).getRGB();
									
									new SegmentProcessor() {
										
										@Override
										protected final void pixel(final int pixel, final int x, final int y) {
											labels[pixel] = rgb;
										}
										
										/**
										 * {@value}.
										 */
										private static final long serialVersionUID = 3483091181451079097L;
										
									}.process(segmentationMask, tileX, tileY);
								}
							}
							
							debugPrint("Labeling done in", timer.toc(), "ms");
							
							debugPrint("Computing visualization...", new Date(timer.tic()));
							
							for (int tileY = 0; tileY + 2 < imageHeight; tileY += s) {
								final int tileLastY = imageHeight <= tileY + s + 2 ? imageHeight - 1 : min(imageHeight - 1, tileY + s);
								
								for (int tileX = 0; tileX + 2 < imageWidth; tileX += s) {
									final int tileLastX = imageWidth <= tileX + s + 2 ? imageWidth - 1 : min(imageWidth - 1, tileX + s);
									final int rgb = labels[tileY * imageWidth + tileX];
									
									if (fillSegments) {
										new SegmentProcessor() {
											
											@Override
											protected final void pixel(final int pixel, final int x, final int y) {
													imageView.getBuffer().getImage().setRGB(x, y, rgb);
											}
											
											/**
											 * {@value}.
											 */
											private static final long serialVersionUID = -1650602767179074395L;
											
										}.process(segmentationMask, tileX, tileY);
									}
									
									if (segmentLocatorColor != null) {
										g.setColor(segmentLocatorColor);
										g.drawOval(tileX - 1, tileY - 1, 2, 2);
									}
									
									if (regionSeparationColor != null) {
										final int northY = tileY;
										final int westX = tileX;
										final int eastX = tileLastX;
										final int southY = tileLastY;
										final int tileIndex = tileY / s * horizontalTileCount + tileX / s;
										final int northX = northXs[tileIndex];
										final int westY = westYs[tileIndex];
										final int southX = northXs[tileIndex + horizontalTileCount];
										final int eastY = westYs[tileIndex + 1];
										final XYW intersection = intersection(xyw(northX, northY), xyw(southX, southY), xyw(westX, westY), xyw(eastX, eastY));
										final int intersectionX = intersection.getUnscaledX();
										final int intersectionY = intersection.getUnscaledY();
										
										g.setColor(regionSeparationColor);
										
										if (rgb != labels[tileLastX + imageWidth * tileY]) {
											g.drawLine(northX, northY, intersectionX, intersectionY);
										}
										
										if (rgb != labels[tileX + imageWidth * tileLastY]) {
											g.drawLine(westX, westY, intersectionX, intersectionY);
										}
										
										if (labels[tileX + imageWidth * tileLastY] != labels[tileLastX + imageWidth * tileLastY]) {
											g.drawLine(intersectionX, intersectionY, southX, southY);
										}
										
										if (labels[tileLastX + imageWidth * tileY] != labels[tileLastX + imageWidth * tileLastY]) {
											g.drawLine(intersectionX, intersectionY, eastX, eastY);
										}
									}
								}
							}
							
							debugPrint("Computing visualization done in", timer.toc(), "ms");
							debugPrint("All done in", timer.getTotalTime(), "ms");
						}
					}
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = -8170474943200742892L;
				
			};
			
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
			private static final long serialVersionUID = -6497489818537320168L;
			
		};
		
		show(imageView, this.getClass().getSimpleName(), true);
	}
	
	public static final XYW intersection(final XYW line1Point1, final XYW line1Point2, final XYW line2Point1, final XYW line2Point2) {
		final XYW line1 = line1Point1.cross(line1Point2);
		final XYW line2 = line2Point1.cross(line2Point2);
		
		assert line1.dot(line1Point1) == 0;
		assert line1.dot(line1Point2) == 0;
		assert line2.dot(line2Point1) == 0;
		assert line2.dot(line2Point2) == 0;
		
		return line1.cross(line2);
	}
	
	public static final Color quantize(final Color color, final int q) {
		final int quantizationMask = ((((~0) << q) & 0xFF) * 0x00010101) | 0xFF000000;
		
		return new Color(color.getRGB() & quantizationMask);
	}
		
	public static final Color quantize(final Color color,
			final int[] redPrototypes, final int[] greenPrototypes, final int[] bluePrototypes) {
		return new Color(
				getNearestNeighbor(color.getRed(), redPrototypes),
				getNearestNeighbor(color.getGreen(), greenPrototypes),
				getNearestNeighbor(color.getBlue(), bluePrototypes));
	}
	
	public static final int getNearestNeighbor(final int value, final int[] prototypes) {
		int result = value;
		int nearestDistance = Integer.MAX_VALUE;
		
		for (final int prototype : prototypes) {
			final int distance = abs(value - prototype);
			
			if (distance < nearestDistance) {
				nearestDistance = distance;
				result = prototype;
			}
		}
		
		return result;
	}
	
	public static final void computePrototypes(final Color[] sortedColors, final int channelOffset,
			final int q, final int[] prototypes) {
		final int colorCount = sortedColors.length;
		final int chunkSize = (colorCount + q - 1) / q;
		
		for (int chunkIndex = 0, prototypeIndex = 0; chunkIndex < colorCount; chunkIndex += chunkSize, ++prototypeIndex) {
			final int nextChunkIndex = min(chunkIndex + chunkSize, colorCount);
			
			for (int colorIndex = chunkIndex; colorIndex < nextChunkIndex; ++colorIndex) {
				prototypes[prototypeIndex] += (sortedColors[colorIndex].getRGB() >> channelOffset) & 0xFF;
			}
			
			final int actualChunkSize = nextChunkIndex - chunkIndex;
			
			if (1 < actualChunkSize) {
				prototypes[prototypeIndex] /= actualChunkSize;
			}
		}
	}
	
	public static final Comparator<Color> COLOR_RED_COMPARATOR = new Comparator<Color>() {
		
		@Override
		public final int compare(final Color color1, final Color color2) {
			return color1.getRed() - color2.getRed();
		}
		
	};
	
	public static final Comparator<Color> COLOR_GREEN_COMPARATOR = new Comparator<Color>() {
		
		@Override
		public final int compare(final Color color1, final Color color2) {
			return color1.getGreen() - color2.getGreen();
		}
		
	};
	
	public static final Comparator<Color> COLOR_BLUE_COMPARATOR = new Comparator<Color>() {
		
		@Override
		public final int compare(final Color color1, final Color color2) {
			return color1.getBlue() - color2.getBlue();
		}
		
	};
	
	public static final int findMaximumGradientX(final BufferedImage image, final int y, final int firstX, final int lastX) {
		int maximumGradient = 0;
		int result = (firstX + lastX) / 2;
		
		for (int x = firstX; x <= lastX; ++x) {
			final int gradient = getColorGradient(image, x, y);
			
			if (maximumGradient < gradient) {
				maximumGradient = gradient;
				result = x;
			}
		}
		
		return result;
	}
	
	public static final int findMaximumGradientY(final BufferedImage image, final int x, final int firstY, final int lastY) {
		int maximumGradient = 0;
		int result = (firstY + lastY) / 2;
		
		for (int y = firstY; y <= lastY; ++y) {
			final int gradient = getColorGradient(image, x, y);
			
			if (maximumGradient < gradient) {
				maximumGradient = gradient;
				result = y;
			}
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2014-02-24)
	 */
	public static final class ComputeMeanColor extends SegmentProcessor {
		
		private final BufferedImage image;
		
		private final Color[] color;
		
		private int red = 0;
		
		private int green = 0;
		
		private int blue = 0;
		
		public ComputeMeanColor(final BufferedImage image, final Color[] color) {
			this.image = image;
			this.color = color;
		}
		
		@Override
		protected final void pixel(final int pixel, final int x, final int y) {
			final int rgb = this.image.getRGB(x, y);
			this.red += (rgb >> 16) & 0xFF;
			this.green += (rgb >> 8) & 0xFF;
			this.blue += (rgb >> 0) & 0xFF;
		}
		
		@Override
		protected final void afterPixels() {
			final int pixelCount = this.getPixelCount();
			
			if (0 < pixelCount) {
				this.red = (this.red / pixelCount) << 16;
				this.green = (this.green / pixelCount) << 8;
				this.blue = (this.blue / pixelCount) << 0;
			}
			
			this.color[0] = new Color(this.red | this.green | this.blue);
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = 8855412064679395641L;
		
	}

	/**
	 * @author codistmonk (creation 2014-02-24)
	 */
	public static abstract class SegmentProcessor implements Serializable {
		
		private int pixelCount;
		
		public final SegmentProcessor process(final BufferedImage segmentation, final int tileX, final int tileY) {
			final SchedulingData schedulingData = getOrCreate(reusableSchedulingData, currentThread(), SchedulingData.FACTORY);
			final IntList todo = schedulingData.getTodo();
			final BitSet done = schedulingData.getDone();
			final int w = segmentation.getWidth();
			final int h = segmentation.getHeight();
			this.pixelCount = 0;
			
			todo.clear();
			done.clear();
			todo.add(tileY * w + tileX);
			
			this.beforePixels();
			
			while (!todo.isEmpty()) {
				final int pixel = todo.remove(0);
				final int x = pixel % w;
				final int y = pixel / w;
				
				++this.pixelCount;
				done.set(pixel);
				
				this.pixel(pixel, x, y);
				
				if (0 < y && !done.get(pixel - w) && (segmentation.getRGB(x, y - 1) & 0x00FFFFFF) == 0) {
					todo.add(pixel - w);
				}
				
				if (0 < x && !done.get(pixel - 1) && (segmentation.getRGB(x - 1, y) & 0x00FFFFFF) == 0) {
					todo.add(pixel - 1);
				}
				
				if (x + 1 < w && !done.get(pixel + 1) && (segmentation.getRGB(x + 1, y) & 0x00FFFFFF) == 0) {
					todo.add(pixel + 1);
				}
				
				if (y + 1 < h && !done.get(pixel + w) && (segmentation.getRGB(x, y + 1) & 0x00FFFFFF) == 0) {
					todo.add(pixel + w);
				}
			}
			
			this.afterPixels();
			
			return this;
		}
		
		public final int getPixelCount() {
			return this.pixelCount;
		}
		
		protected void beforePixels() {
			// NOP
		}
		
		protected abstract void pixel(int pixel, int x, int y);
		
		protected void afterPixels() {
			// NOP
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -5860299062780405885L;
		
		private static final Map<Thread, SchedulingData> reusableSchedulingData = new WeakHashMap<Thread, SchedulingData>();
		
		/**
		 * @author codistmonk (creation 2014-02-26)
		 */
		public static final class SchedulingData implements Serializable {
			
			private final IntList todo;
			
			private final BitSet done;
			
			public SchedulingData() {
				this.todo = new IntList();
				this.done = new BitSet();
			}
			
			public final IntList getTodo() {
				return this.todo;
			}
			
			public final BitSet getDone() {
				return this.done;
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -3771171821850295230L;
			
			public static final DefaultFactory<SchedulingData> FACTORY = DefaultFactory.forClass(SchedulingData.class);
			
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2014-02-25)
	 */
	public static final class XYW implements Serializable {
		
		private final int x;
		
		private final int y;
		
		private final int w;
		
		public XYW(final int x, final int y, final int w) {
			this.x = x;
			this.y = y;
			this.w = w;
		}
		
		public final int getX() {
			return this.x;
		}
		
		public final int getY() {
			return this.y;
		}
		
		public final int getW() {
			return this.w;
		}
		
		public final int getUnscaledX() {
			return this.getX() / this.getW();
		}
		
		public final int getUnscaledY() {
			return this.getY() / this.getW();
		}
		
		public final int dot(final XYW that) {
			return this.getX() * that.getX() + this.getY() * that.getY() + this.getW() * that.getW();
		}
		
		public final XYW cross(final XYW that) {
			return new XYW(
					det(this.getY(), this.getW(), that.getY(), that.getW()),
					det(this.getW(), this.getX(), that.getW(), that.getX()),
					det(this.getX(), this.getY(), that.getX(), that.getY()));
		}
		
		@Override
		public final String toString() {
			return "[" + this.getX() + " " + this.getY() + " " + this.getW() + "]";
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -3326572044424114312L;
		
		public static final int det(final int a, final int b, final int c, final int d) {
			return a * d - b * c;
		}
		
		public static final XYW xyw(final int x, final int y) {
			return new XYW(x, y, 1);
		}
		
	}
	
}
