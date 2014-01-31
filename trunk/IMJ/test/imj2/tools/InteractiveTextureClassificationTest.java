package imj2.tools;

import static imj2.tools.TextureGradientTest.BIN_COUNT;
import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Color.CYAN;
import static java.awt.Color.GRAY;
import static java.awt.Color.GREEN;
import static java.awt.Color.MAGENTA;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import static java.awt.Color.YELLOW;
import static java.lang.Math.min;
import static net.sourceforge.aprog.swing.SwingTools.show;
import static net.sourceforge.aprog.tools.Tools.cast;
import static net.sourceforge.aprog.tools.Tools.invoke;

import imj2.tools.Image2DComponent.Painter;
import imj2.tools.RegionShrinkingTest.AutoMouseAdapter;
import imj2.tools.RegionShrinkingTest.SimpleImageView;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-01-30)
 */
public final class InteractiveTextureClassificationTest {
	
	@Test
	public final void test() {
		final SimpleImageView imageView = new SimpleImageView();
		
		new AutoMouseAdapter(imageView.getImageHolder()) {
			
			private final Painter<SimpleImageView> painter = new Painter<SimpleImageView>() {
				
				@Override
				public final void paint(final Graphics2D g, final SimpleImageView component,
						final int width, final int height) {
					refreshLabels(component.getImage());
					
					final BufferedImage labels = getLabels();
					final int labelsWidth = labels.getWidth();
					final int labelsHeight = labels.getHeight();
					final int cellOutlineSize = CELL_SIZE - 1;
					
					for (int labelsY = 0; labelsY < labelsHeight; ++labelsY) {
						for (int labelsX = 0; labelsX < labelsWidth; ++labelsX) {
							final int rgb = labels.getRGB(labelsX, labelsY);
							final int left = labelsX * CELL_SIZE;
							final int top = labelsY * CELL_SIZE;
							final int right = left + cellOutlineSize;
							final int bottom = top + cellOutlineSize;
							
							g.setColor(new Color(rgb));
							
							if (labelsX == 0 || rgb != labels.getRGB(labelsX - 1, labelsY)) {
								g.drawLine(left, top, left, bottom);
							}
							
							if (labelsY == 0 || rgb != labels.getRGB(labelsX, labelsY - 1)) {
								g.drawLine(left, top, right, top);
							}
							
							if (labelsX == labelsWidth - 1 || rgb != labels.getRGB(labelsX + 1, labelsY)) {
								g.drawLine(right, top, right, bottom);
							}
							
							if (labelsY == labelsHeight - 1 || rgb != labels.getRGB(labelsX, labelsY + 1)) {
								g.drawLine(left, bottom, right, bottom);
							}
						}
					}
				}
				
				/**
				 * {@value}.
				 */
				private static final long serialVersionUID = 7956137594172092843L;
				
			};
			
			private BufferedImage labels;
			
			private int userLabelColorIndex;
			
			private boolean interactive;
			
			private boolean popup;
			
			{
				this.userLabelColorIndex = 3;
				this.interactive = false;
				
				final int n = CELL_SIZE * CELL_SIZE;
				final int[] blackHistogram = set(new int[BIN_COUNT], 0, n);
				final int[] rainbowHistogram = fill(blackHistogram.clone(), n / BIN_COUNT);
				final int[] whiteHistogram = set(new int[TextureGradientTest.BIN_COUNT], BIN_COUNT - 1, n);
				
				addLabel(blackHistogram, BLACK);
				addLabel(rainbowHistogram, GRAY);
				addLabel(whiteHistogram, WHITE);
				
				imageView.getPainters().add(this.painter);
			}
			
			public final BufferedImage getLabels() {
				return this.labels;
			}
			
			public final Color getUserLabelColor() {
				return COLORS[this.userLabelColorIndex];
			}
			
			public final void refreshLabels(final BufferedImage image) {
				final int imageWidth = image.getWidth();
				final int imageHeight = image.getHeight();
				final int labelsWidth = imageWidth / CELL_SIZE;
				final int labelsHeight = imageHeight / CELL_SIZE;
				
				if (this.getLabels() == null ||
						this.getLabels().getWidth() != labelsWidth || this.getLabels().getHeight() != labelsHeight) {
					this.labels = new BufferedImage(labelsWidth, labelsHeight, BufferedImage.TYPE_3BYTE_BGR);
				}
				
				final BufferedImage labels = this.getLabels();
				final int[] histogram = new int[BIN_COUNT];
				
				for (int labelsY = 0; labelsY < labelsHeight; ++labelsY) {
					for (int labelsX = 0; labelsX < labelsWidth; ++labelsX) {
						TextureGradientTest.computeHistogram(image, labelsX * CELL_SIZE, labelsY * CELL_SIZE,
								CELL_SIZE, CELL_SIZE, histogram);
						
						final Color labelColor = LABEL_COLORS.get(LABEL_HISTOGRAMS.get(getLabel(histogram)));
						try {
							labels.setRGB(labelsX, labelsY, labelColor.getRGB());
						} catch (final Exception exception) {
							Tools.debugPrint(getLabel(histogram), labelColor, LABEL_HISTOGRAMS.get(getLabel(histogram)));
							return;
						}
					}
				}
			}
			
			@Override
			public final void mouseExited(final MouseEvent event) {
				if (this.interactive) {
					this.interactive = false;
					removeLastLabelHistogram();
					imageView.refreshBuffer();
				}
			}
			
			@Override
			public final void mousePressed(final MouseEvent event) {
				this.popup = event.isPopupTrigger();
			}
			
			@Override
			public final void mouseReleased(final MouseEvent event) {
				this.popup |= event.isPopupTrigger();
			}
			
			@Override
			public final void mouseClicked(final MouseEvent event) {
				if (this.popup) {
					this.popup = false;
					this.userLabelColorIndex = (this.userLabelColorIndex + 1) % COLORS.length;
				} else {
					this.interactive = false;
				}
				
				this.mouseMoved(event);
			}
			
			@Override
			public final void mouseMoved(final MouseEvent event) {
				final BufferedImage image = imageView.getImage();
				
				if (image == null) {
					return;
				}
				
				final int[] histogram = new int[BIN_COUNT];
				final int labelsX = event.getX() / CELL_SIZE;
				final int labelsY = event.getY() / CELL_SIZE;
				
				TextureGradientTest.computeHistogram(image, labelsX * CELL_SIZE, labelsY * CELL_SIZE,
						CELL_SIZE, CELL_SIZE, histogram);
				
				if (this.interactive) {
					removeLastLabelHistogram();
				} else {
					this.interactive = true;
				}
				
				addLabel(histogram, this.getUserLabelColor());
				imageView.refreshBuffer();
			}
			
			@Override
			protected final void cleanup() {
				imageView.getPainters().remove(this.painter);
			}
			
			/**
			 * {@value}.
			 */
			private static final long serialVersionUID = -910005796474041331L;
			
		};
		
		show(imageView, "Simple Image View", true);
	}
	
	/**
	 * {@value}.
	 */
	public static final int CELL_SIZE = 16;
	
	static final List<ArrayHolder<int[]>> LABEL_HISTOGRAMS = new ArrayList<ArrayHolder<int[]>>();
	
	static final Map<ArrayHolder<int[]>, Color> LABEL_COLORS = new HashMap<ArrayHolder<int[]>, Color>();
	
	static final Color[] COLORS = { BLACK, GRAY, WHITE, YELLOW, RED, GREEN, BLUE, CYAN, MAGENTA };
	
	public static final int[] set(final int[] array, final int index, final int value) {
		array[index] = value;
		
		return array;
	}
	
	public static final int[] fill(final int[] array, final int value) {
		Arrays.fill(array, value);
		
		return array;
	}
	
	static final void removeLastLabelHistogram() {
		final ArrayHolder<int[]> h = LABEL_HISTOGRAMS.remove(LABEL_HISTOGRAMS.size() - 1);
		
		if (!LABEL_HISTOGRAMS.contains(h)) {
			LABEL_COLORS.remove(h);
		}
	}
	
	static final int getLabel(final int[] histogram) {
		int result = -1;
		float closestDistance = Float.POSITIVE_INFINITY;
		final int n = LABEL_HISTOGRAMS.size();
		
		for (int label = 0; label < n; ++label) {
			final float distance = TextureGradientTest.computeHistogramDistance(histogram,
					LABEL_HISTOGRAMS.get(label).getArray());
			
			if (distance < closestDistance) {
				result = label;
				closestDistance = distance;
			}
		}
		
		return result;
	}
	
	static final void addLabel(final int[] histogram, final Color color) {
		final ArrayHolder<int[]> h = new ArrayHolder<int[]>(histogram);
		
		LABEL_HISTOGRAMS.add(h);
		LABEL_COLORS.put(h, color);
		
		assert LABEL_COLORS.get(h).equals(color);
		assert LABEL_COLORS.get(new ArrayHolder<int[]>(histogram)).equals(color);
	}
	
	/**
	 * @author codistmonk (creation 2014-01-30)
	 *
	 * @param <A>
	 */
	public static final class ArrayHolder<A> implements Serializable, Comparable<ArrayHolder<A>> {
		
		private final A array;
		
		public ArrayHolder(final A array) {
			this.array = array;
		}
		
		public final A getArray() {
			return this.array;
		}
		
		public final int getElementCount() {
			return Array.getLength(this.getArray());
		}
		
		public final <T> T getElement(final int index) {
			return (T) Array.get(this.getArray(), index);
		}
		
		@Override
		public final int compareTo(final ArrayHolder<A> that) {
			final int thisLength = this.getElementCount();
			final int thatLength = that.getElementCount();
			final int n = min(thisLength, thatLength);
			
			for (int i = 0; i < n; ++i) {
				final Object thisElement = this.getElement(i);
				final Object thatElement = that.getElement(i);
				final int hashDifference = thisElement.hashCode() - thatElement.hashCode();
				
				if (hashDifference != 0) {
					return hashDifference;
				}
			}
			
			return thisLength - thatLength;
		}
		
		@Override
		public final boolean equals(final Object object) {
			final ArrayHolder<A> that = cast(this.getClass(), object);
			
			return that != null && this.getArray().getClass().equals(that.getArray().getClass()) &&
					(Boolean) invoke(Arrays.class, "equals", this.getArray(), that.getArray());
		}
		
		@Override
		public final int hashCode() {
			return invoke(Arrays.class, "hashCode", (Object) this.getArray());
		}
		
		@Override
		public final String toString() {
			return invoke(Arrays.class, "toString", (Object) this.getArray());
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -2560117214642824644L;
		
	}
	
}
