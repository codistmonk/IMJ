package imj2.tools;

import static imj2.tools.BitwiseQuantizationTest.min;
import static imj2.tools.BitwiseQuantizationTest.packCIELAB;
import static imj2.tools.BitwiseQuantizationTest.rgbToRGB;
import static imj2.tools.BitwiseQuantizationTest.rgbToXYZ;
import static imj2.tools.BitwiseQuantizationTest.xyzToCIELAB;
import static imj2.tools.MultiresolutionSegmentationTest.nextLOD;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.fill;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.debug;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getOrCreate;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import imj2.tools.BitwiseQuantizationTest.ColorQuantizer;
import imj2.tools.Image2DComponent.Painter;

import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-04-11)
 */
public final class HighSegmentationLowLabelingTest {

	@Test
	public final void test() {
		final Color contourColor = Color.GREEN;
		
		final SimpleImageView imageView = new SimpleImageView();
		
		imageView.getPainters().add(new Painter<SimpleImageView>() {
			
			private Canvas labels0 = new Canvas();
			
			private Canvas labels1 = new Canvas();
			
			private Canvas labels2 = new Canvas();
			
			private BufferedImage image0;
			
			private BufferedImage image1;
			
			private BufferedImage image2;
			
			@Override
			public final void paint(final Graphics2D g, final SimpleImageView component,
					final int width, final int height) {
				
				final TicToc timer = new TicToc();
				
				debugPrint("Initializing...", new Date(timer.tic()));
				
				final ColorQuantizer quantizer = BitwiseQuantizationTest.quantizers.get(17);
				
				if (this.image0 != imageView.getImage()) {
					this.image0 = imageView.getImage();
				}
				
				this.image1 = nextLOD(this.image0);
				this.image2 = nextLOD(this.image1);
				
				final int w0 = this.image0.getWidth();
				final int h0 = this.image0.getHeight();
				
				if (false) {
					for (int y = 0; y < h0; ++y) {
						for (int x = 0; x < w0; ++x) {
							final int[] rgb = rgbToRGB(this.image0.getRGB(x, y), new int[3]);
							final float[] cielab = xyzToCIELAB(rgbToXYZ(rgb, new float[3]));
							
							imageView.getBufferImage().setRGB(x, y, packCIELAB(cielab));
						}
					}
					
					return;
				}
				
				final int w1 = this.image1.getWidth();
				final int h1 = this.image1.getHeight();
				final int w2 = this.image2.getWidth();
				final int h2 = this.image2.getHeight();
				
				this.labels0.setFormat(w0, h0, BufferedImage.TYPE_3BYTE_BGR);
				this.labels1.setFormat(w1, h1, BufferedImage.TYPE_3BYTE_BGR);
				this.labels2.setFormat(w2, h2, BufferedImage.TYPE_3BYTE_BGR);
				
				this.pack(this.image0, quantizer, this.labels0.getImage());
				this.pack(this.image1, quantizer, this.image1);
				this.pack(this.image2, quantizer, this.image2);
				
//				SwingTools.show(this.image2, "LOD2", false);
				
				debugPrint(timer.toc());
				
				debugPrint("Labeling...", new Date(timer.tic()));
				
				this.setLabels(this.labels0.getImage());
				
				debugPrint(timer.toc());
				
				debugPrint("Collecting texture distributions...", new Date(timer.tic()));
				
				final int windowSize = 3;
				final int[] histogram = new int[quantizer.getBinCount()];
				final int[][] binHistograms = computeBinHistograms(this.image1, histogram, windowSize);
				
				debugPrint(timer.toc());
				
				debugPrint("Clustering texture distributions...", new Date(timer.tic()));
				
				final int[] quantas = repeat(histogram.length, windowSize * windowSize);
				
				debugPrint(Arrays.toString(quantas));
				reduce(quantas, binHistograms, histogram.length * windowSize * windowSize - 30);
				debugPrint(Arrays.toString(quantas));
				
				debugPrint(timer.toc());
				
				debugPrint("Quantizing textures...", new Date(timer.tic()));
				
				this.quantize(this.image1, windowSize, histogram, quantas, this.labels1.getImage());
				
//				SwingTools.show(this.labels2.getImage(), "LOD2", false);
				
				debugPrint(timer.toc());
				
				debugPrint("Applying textures...", new Date(timer.tic()));
				
				this.adjustLabels(this.labels1.getImage(), this.labels0.getImage());
				
//				SwingTools.show(this.labels0.getImage(), "LOD0", false);
				
				debugPrint(timer.toc());
				
				debugPrint("Drawing...", new Date(timer.tic()));
				
				this.drawContours(contourColor, imageView.getBufferImage());
				
//				for (int y = 0; y < this.labels2.getHeight(); ++y) {
//					for (int x = 0; x < this.labels2.getWidth(); ++x) {
//						imageView.getBufferGraphics().setColor(new Color(0xFF000000 | (this.labels2.getImage().getRGB(x, y))));
//						imageView.getBufferGraphics().drawRect(x * 4, y * 4, 4, 4);
//					}
//				}
				
				debugPrint(timer.toc());
			}

			private final void quantize(final BufferedImage source,
					final int windowSize, final int[] histogram, final int[] quantas,
					final BufferedImage target) {
				final int w = target.getWidth();
				final int h = target.getHeight();
				final Map<String, Integer> quantizedHistogramIds = new HashMap<String, Integer>();
				
				for (int y = 0; y + windowSize <= h; ++y) {
					for (int x = 0; x + windowSize <= w; ++x) {
						computeHistogram(source, x, y, windowSize, windowSize, histogram);
						final int label = quantize(histogram, windowSize * windowSize, quantas, quantizedHistogramIds);
//						final int label = this.image1.getRGB(x, y);
						target.setRGB(x, y, label);
						
//						imageView.getBufferImage().setRGB(2 * x + 0, 2 * y + 0, label << 3);
//						imageView.getBufferImage().setRGB(2 * x + 1, 2 * y + 0, label << 3);
//						imageView.getBufferImage().setRGB(2 * x + 0, 2 * y + 1, label << 3);
//						imageView.getBufferImage().setRGB(2 * x + 1, 2 * y + 1, label << 3);
					}
				}
			}

			private final int[][] computeBinHistograms(final BufferedImage quantizedImage, final int[] histogram, final int windowSize) {
				final int w = quantizedImage.getWidth();
				final int h = quantizedImage.getHeight();
				final int[][] result = new int[histogram.length][windowSize * windowSize + 1];
				
				for (int y = 0; y + windowSize <= h; ++y) {
					for (int x = 0; x + windowSize <= w; ++x) {
						computeHistogram(quantizedImage, x, y, windowSize, windowSize, histogram);
						
						for (int i = 0; i < histogram.length; ++i) {
							++result[i][histogram[i]];
						}
					}
				}
				
				return result;
			}
			
			private final int quantize(final int[] histogram, final int pixelCount, final int[] quantas, final Map<String, Integer> quantizedHistogramIds) {
				final int n = histogram.length;
				String key = "";
				
				for (int i = 0; i < n; ++i) {
					key += (histogram[i] * quantas[i] / pixelCount) + " ";
				}
				
				Integer result = quantizedHistogramIds.get(key);
				
				if (result == null) {
					result = quantizedHistogramIds.size();
					quantizedHistogramIds.put(key, result);
				}
				
				return result;
			}
			
			private final int[] repeat(final int n, final int value) {
				final int[] result = new int[n];
				
				fill(result, value);
				
				return result;
			}
			
			private final void reduce(final int[] quantas, final int[][] binHistograms, final int amount) {
				final int n = quantas.length;
				
				for (int j = 0; j < amount; ++j) {
					double minimumError = Double.POSITIVE_INFINITY;
					int index = -1;
					
					for (int i = 0; i < n; ++i) {
						if (0 < quantas[i]) {
							final double e = this.computeQuantizationError(binHistograms, i, quantas[i] - 1);
							
							if (e < minimumError) {
								minimumError = e;
								index = i;
							}
						}
					}
					
					if (0 <= index) {
						--quantas[index];
					} else {
						break;
					}
				}
			}
			
			private final double computeQuantizationError(final int[][] binHistogram, final int bin, final int quanta) {
				double result = 0.0;
				
				final int max = binHistogram[bin].length - 1;
				
				for (int i = 0; i <= max; ++i) {
					final int value = binHistogram[bin][i];
					final int quantizedValue = value * quanta / max;
					result += abs(quantizedValue - value);
				}
				
				return result;
			}
			
			private final void drawContours(final Color contourColor, final BufferedImage buffer) {
				final int w = buffer.getWidth();
				final int h = buffer.getHeight();
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						int north = 0;
						int west = 0;
						int east = 0;
						int south = 0;
						
						if (0 < y) {
							north = this.labels0.getImage().getRGB(x, y - 1);
						}
						
						if (0 < x) {
							west = this.labels0.getImage().getRGB(x - 1, y);
						}
						
						if (x + 1 < w) {
							east = this.labels0.getImage().getRGB(x + 1, y);
						}
						
						if (y + 1 < h) {
							south = this.labels0.getImage().getRGB(x, y + 1);
						}
						
						final int center = this.labels0.getImage().getRGB(x, y);
						
						if (min(north, west, east, south) < center) {
							buffer.setRGB(x, y, contourColor.getRGB());
						}
					}
				}
			}

			private void setLabels(final BufferedImage image) {
				final int w = image.getWidth();
				final int h = image.getHeight();
				final SchedulingData schedulingData = new SchedulingData();
				int totalPixelCount = 0;
				
				for (int y = 0, pixel = 0, labelId = 0xFF000000; y < h; ++y) {
					for (int x = 0; x < w; ++x, ++pixel) {
						if (!schedulingData.getDone().get(pixel)) {
							schedulingData.getTodo().add(pixel);
							
							final int rgb = image.getRGB(x, y);
							
							for (int i = 0; i < schedulingData.getTodo().size(); ++i) {
								final int p = schedulingData.getTodo().get(i);
								
								image.setRGB(p % w, p / w, labelId);
								this.process(w, h, schedulingData, p % w, p / w, p, rgb);
							}
							
							final int componentPixelCount = schedulingData.getTodo().size();
							
							++labelId;
							totalPixelCount += componentPixelCount;
							
							schedulingData.getTodo().clear();
						}
					}
				}
				
				schedulingData.getDone().clear();
				
				if (w * h != totalPixelCount) {
					System.err.println(debug(Tools.DEBUG_STACK_OFFSET, "Error:", "expected:", w * h, "actual:", totalPixelCount));
				}
			}
			
			private void adjustLabels(final BufferedImage source, final BufferedImage target) {
				final SchedulingData schedulingData = new SchedulingData();
				final DefaultFactory<AtomicInteger> factory = DefaultFactory.forClass(AtomicInteger.class);
				final int w = target.getWidth();
				final int h = target.getHeight();
				final int s = target.getWidth() / source.getWidth();
				
				for (int y = 0, pixel = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x, ++pixel) {
						if (!schedulingData.getDone().get(pixel)) {
							schedulingData.getTodo().add(pixel);
							
							final int rgb = target.getRGB(x, y);
							
							for (int i = 0; i < schedulingData.getTodo().size(); ++i) {
								final int p = schedulingData.getTodo().get(i);
								
								this.process(w, h, schedulingData, p % w, p / w, p, rgb);
							}
							
							final Map<Integer, AtomicInteger> lowLabeling = new HashMap<Integer, AtomicInteger>();
							
							for (int i = 0; i < schedulingData.getTodo().size(); ++i) {
								final int p = schedulingData.getTodo().get(i);
								final int xx = (p % w) / s;
								final int yy = (p / w) / s;
								
								if (xx < source.getWidth() && yy < source.getHeight()) {
									getOrCreate(lowLabeling, source.getRGB(xx, yy), factory).incrementAndGet();
								}
							}
							
							int newLabel = rgb;
							int newLabelCount = 0;
							
							for (final Map.Entry<Integer, AtomicInteger> entry : lowLabeling.entrySet()) {
								final int count = entry.getValue().get();
								
								if (newLabelCount < count) {
									newLabelCount = count;
									newLabel = entry.getKey();
								}
							}
							
							for (int i = 0; i < schedulingData.getTodo().size(); ++i) {
								final int p = schedulingData.getTodo().get(i);
								target.setRGB(p % w, p / w, newLabel);
							}
							
							schedulingData.getTodo().clear();
						}
					}
				}
			}
			
			private final void pack(final BufferedImage image, final ColorQuantizer quantizer, final BufferedImage labels) {
				final int w = image.getWidth();
				final int h = image.getHeight();
				
				for (int y = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x) {
						labels.setRGB(x, y, quantizer.pack(image.getRGB(x, y)));
					}
				}
			}
			
			private final void process(final int w, final int h,
					final SchedulingData schedulingData, final int x, final int y,
					final int pixel, final int labelRGB) {
				schedulingData.getDone().set(pixel);
				
				if (0 < y && this.labels0.getImage().getRGB(x, y - 1) == labelRGB) {
					final int neighbor = pixel - w;
					
					if (!schedulingData.getDone().get(neighbor)) {
						schedulingData.getDone().set(neighbor);
						schedulingData.getTodo().add(neighbor);
					}
				}
				
				if (0 < x && this.labels0.getImage().getRGB(x - 1, y) == labelRGB) {
					final int neighbor = pixel - 1;
					
					if (!schedulingData.getDone().get(neighbor)) {
						schedulingData.getDone().set(neighbor);
						schedulingData.getTodo().add(neighbor);
					}
				}
				
				if (x + 1 < w && this.labels0.getImage().getRGB(x + 1, y) == labelRGB) {
					final int neighbor = pixel + 1;
					
					if (!schedulingData.getDone().get(neighbor)) {
						schedulingData.getDone().set(neighbor);
						schedulingData.getTodo().add(neighbor);
					}
				}
				
				if (y + 1 < h && this.labels0.getImage().getRGB(x, y + 1) == labelRGB) {
					final int neighbor = pixel + w;
					
					if (!schedulingData.getDone().get(neighbor)) {
						schedulingData.getDone().set(neighbor);
						schedulingData.getTodo().add(neighbor);
					}
				}
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -5115463345876567457L;
			
		});
		
		show(imageView, this.getClass().getSimpleName(), true);
	}
	
	public static final int[] computeHistogram(final BufferedImage image,
			final int x, final int y, final int width, final int height, final int[] result) {
		fill(result, 0);
		
		final int firstY = max(0, y);
		final int lastY = min(image.getHeight() - 1, y + height - 1);
		final int firstX = max(0, x);
		final int lastX= min(image.getWidth() - 1, x + width - 1);
		
		for (int yy = firstY; yy <= lastY; ++yy) {
			for (int xx = firstX; xx <= lastX; ++xx) {
				++result[0x00FFFFFF & image.getRGB(xx, yy)];
			}
		}
		
		return result;
	}
	
}
