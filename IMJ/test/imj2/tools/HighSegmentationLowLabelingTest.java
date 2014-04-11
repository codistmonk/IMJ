package imj2.tools;

import static imj2.tools.BitwiseQuantizationTest.min;
import static imj2.tools.MultiresolutionSegmentationTest.nextLOD;
import static imj2.tools.TextureGradientTest.computeHistogram;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.fill;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.debug;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static net.sourceforge.aprog.tools.Tools.getOrCreate;
import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import imj.IntList;
import imj2.tools.BitwiseQuantizationTest.ColorQuantizer;
import imj2.tools.Image2DComponent.Painter;
import net.sourceforge.aprog.swing.SwingTools;
import net.sourceforge.aprog.tools.Factory.DefaultFactory;
import net.sourceforge.aprog.tools.MathTools;
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
			
			private BufferedImage image0;
			
			private BufferedImage image1;
			
			@Override
			public final void paint(final Graphics2D g, final SimpleImageView component,
					final int width, final int height) {
				final TicToc timer = new TicToc();
				
				timer.tic();
				
				final ColorQuantizer quantizer = BitwiseQuantizationTest.quantizers.get(17);
				
				if (this.image0 != imageView.getImage()) {
					this.image0 = imageView.getImage();
				}
				
				this.image1 = nextLOD(this.image0);
				
				final int w0 = this.image0.getWidth();
				final int h0 = this.image0.getHeight();
				final int w1 = this.image1.getWidth();
				final int h1 = this.image1.getHeight();
				
				this.labels0.setFormat(w0, h0, BufferedImage.TYPE_3BYTE_BGR);
				this.labels1.setFormat(w1, h1, BufferedImage.TYPE_3BYTE_BGR);
				
				this.initializeLabels(this.image0, quantizer, this.labels0.getImage());
				this.initializeLabels(this.image1, quantizer, this.image1);
				
				debugPrint(timer.toc());
				
				this.setLabels(w0, h0);
				
				debugPrint(timer.toc());
				
				final int windowSize = 5;
				final int[] histogram = new int[1 + quantizer.getBinCount()];
				final int[][] binHistograms = new int[histogram.length][windowSize * windowSize + 1];
				
				for (int y = 0; y + windowSize <= h1; ++y) {
					for (int x = 0; x + windowSize <= w1; ++x) {
						computeHistogram(this.image1, x, y, windowSize, windowSize, histogram);
						
						for (int i = 0; i < histogram.length; ++i) {
							++binHistograms[i][histogram[i]];
						}
					}
				}
				
				debugPrint(timer.toc());
				
				final int[] quantas = repeat(histogram.length, windowSize * windowSize);
				
				reduce(quantas, binHistograms, (histogram.length + 1) * windowSize * windowSize - 200);
				
				debugPrint(timer.toc());
				
				final Map<String, Integer> quantizedHistogramIds = new HashMap<String, Integer>();
				
				for (int y = 0; y + windowSize <= h1; ++y) {
					for (int x = 0; x + windowSize <= w1; ++x) {
						computeHistogram(this.image1, x, y, windowSize, windowSize, histogram);
						final int label = quantize(histogram, windowSize * windowSize, quantas, quantizedHistogramIds);
//						final int label = this.image1.getRGB(x, y);
						this.labels1.getImage().setRGB(x, y, label);
						
//						imageView.getBufferImage().setRGB(2 * x + 0, 2 * y + 0, label << 3);
//						imageView.getBufferImage().setRGB(2 * x + 1, 2 * y + 0, label << 3);
//						imageView.getBufferImage().setRGB(2 * x + 0, 2 * y + 1, label << 3);
//						imageView.getBufferImage().setRGB(2 * x + 1, 2 * y + 1, label << 3);
					}
				}
				
				debugPrint(timer.toc());
				
				this.adjustLabels(w0, h0);
				
				debugPrint(timer.toc());
				
				this.drawContours(contourColor, imageView.getBufferImage());
				
				debugPrint(timer.toc());
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
						if (1 < quantas[i]) {
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
						
//						buffer.setRGB(x, y, center << 2);
						
						if (min(north, west, east, south) < center) {
							buffer.setRGB(x, y, contourColor.getRGB());
						}
					}
				}
			}

			private void setLabels(final int w, final int h) {
				final SchedulingData schedulingData = new SchedulingData();
				int totalPixelCount = 0;
				
				for (int y = 0, pixel = 0, labelId = 0xFF000000; y < h; ++y) {
					for (int x = 0; x < w; ++x, ++pixel) {
						if (!schedulingData.getDone().get(pixel)) {
							schedulingData.getTodo().add(pixel);
							
							final int rgb = this.labels0.getImage().getRGB(x, y);
							
							for (int i = 0; i < schedulingData.getTodo().size(); ++i) {
								final int p = schedulingData.getTodo().get(i);
								
								this.labels0.getImage().setRGB(p % w, p / w, labelId);
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
			
			private void adjustLabels(final int w, final int h) {
				final SchedulingData schedulingData = new SchedulingData();
				final DefaultFactory<AtomicInteger> factory = DefaultFactory.forClass(AtomicInteger.class);
				
				for (int y = 0, pixel = 0; y < h; ++y) {
					for (int x = 0; x < w; ++x, ++pixel) {
						if (!schedulingData.getDone().get(pixel)) {
							schedulingData.getTodo().add(pixel);
							
							final int rgb = this.labels0.getImage().getRGB(x, y);
							
							for (int i = 0; i < schedulingData.getTodo().size(); ++i) {
								final int p = schedulingData.getTodo().get(i);
								
								this.process(w, h, schedulingData, p % w, p / w, p, rgb);
							}
							
							final Map<Integer, AtomicInteger> lowLabeling = new HashMap<Integer, AtomicInteger>();
							
							for (int i = 0; i < schedulingData.getTodo().size(); ++i) {
								final int p = schedulingData.getTodo().get(i);
								try {
									getOrCreate(lowLabeling, this.labels1.getImage().getRGB((p % w) / 2, (p / w) / 2), factory).incrementAndGet();
								} catch (final Exception exception) {
//									exception.printStackTrace();
									System.err.println(exception + " " + ((p % w) / 2) + " " + ((p / w) / 2) + " " + this.labels1.getWidth() + " " + this.labels1.getHeight());
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
								this.labels0.getImage().setRGB(p % w, p / w, newLabel);
							}
							
							schedulingData.getTodo().clear();
						}
					}
				}
				
				schedulingData.getDone().clear();
			}
			
			private final void initializeLabels(final BufferedImage image, final ColorQuantizer quantizer, final BufferedImage labels) {
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
