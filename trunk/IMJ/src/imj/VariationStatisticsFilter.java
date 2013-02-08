package imj;

import static imj.IMJTools.alpha;
import static imj.IMJTools.blue;
import static imj.IMJTools.green;
import static imj.IMJTools.red;
import static imj.IMJTools.square;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;

import imj.IMJTools.StatisticsSelector;

/**
 * @author codistmonk (creation 2013-02-08)
 */
public final class VariationStatisticsFilter extends StatisticalFilter {
	
	private final Variation variation;
	
	public VariationStatisticsFilter(final Image image, final StatisticsSelector selector, final Variation variation, final int[] structuringElement) {
		super(image, selector, structuringElement);
		
		this.variation = variation;
	}
	
	public final Variation getVariation() {
		return this.variation;
	}
	
	@Override
	protected final Synthesizer newSynthesizer(final int[] structuringElement) {
		return this.new Selector(structuringElement);
	}
	
	/**
	 * @author codistmonk (creation 2013-02-03)
	 */
	public final class Selector extends StatisticalFilter.Selector {
		
		private int pixel;
		
		private int value;
		
		public Selector(final int... deltas) {
			super(deltas);
		}
		
		@Override
		protected final void reset(final int pixel) {
			super.reset(pixel);
			this.pixel = pixel;
			this.value = VariationStatisticsFilter.this.getImage().getValue(pixel);
		}
		
		@Override
		protected final int getValue(final int pixel, final int value) {
			return VariationStatisticsFilter.this.getVariation().computeValue(this.pixel, this.value, pixel, value);
		}
		
		@Override
		protected final float getFloatValue(final int pixel, final float value) {
			return this.getValue(pixel, (int) value);
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2013-02-08)
	 */
	public static abstract interface Variation {
		
		public abstract int computeValue(int pixel1, int value1, int pixel2, int value2);
		
		/**
		 * @author codistmonk (creation 2013-02-08)
		 */
		public static enum Predefined implements Variation {
			
			DIFFERENCE {
				
				@Override
				public final int computeValue(final int pixel1, final int value1, final int pixel2, final int value2) {
					return value1 - value2;
				}
				
			}, ABSOLUTE_DIFFERENCE {
				
				@Override
				public final int computeValue(final int pixel1, final int value1, final int pixel2, final int value2) {
					return abs(value1 - value2);
				}
				
			}, SUM {
				
				@Override
				public final int computeValue(final int pixel1, final int value1, final int pixel2, final int value2) {
					return value1 + value2;
				}
				
			}, PRODUCT {
				
				@Override
				public final int computeValue(final int pixel1, final int value1, final int pixel2, final int value2) {
					return value1 / value2;
				}
					
			}, MIN {
				
				@Override
				public final int computeValue(final int pixel1, final int value1, final int pixel2, final int value2) {
					return min(value1, value2);
				}
				
			}, MAX {
				
				@Override
				public final int computeValue(final int pixel1, final int value1, final int pixel2, final int value2) {
					return max(value1, value2);
				}
				
			}, RGB_SQUARED_EUCLIDEAN_DISTANCE {
				
				@Override
				public final int computeValue(final int pixel1, final int value1, final int pixel2, final int value2) {
					final int r1 = red(value1);
					final int g1 = green(value1);
					final int b1 = blue(value1);
					final int r2 = red(value2);
					final int g2 = green(value2);
					final int b2 = blue(value2);
					
					return (int) square(r2 - r1) + square(g2 - g1) + square(b2 - b1);
				}
				
			}, RGB_EUCLIDEAN_DISTANCE {
				
				@Override
				public final int computeValue(final int pixel1, final int value1, final int pixel2, final int value2) {
					return (int) sqrt(RGB_SQUARED_EUCLIDEAN_DISTANCE.computeValue(pixel1, value1, pixel2, value2));
				}
				
			}, RGBA_SQUARED_EUCLIDEAN_DISTANCE {
				
				@Override
				public final int computeValue(final int pixel1, final int value1, final int pixel2, final int value2) {
					final int r1 = red(value1);
					final int g1 = green(value1);
					final int b1 = blue(value1);
					final int a1 = alpha(value1);
					final int r2 = red(value2);
					final int g2 = green(value2);
					final int b2 = blue(value2);
					final int a2 = alpha(value2);
					
					return square(r2 - r1) + square(g2 - g1) + square(b2 - b1) + square(a2 - a1);
				}
				
			}, RGBA_EUCLIDEAN_DISTANCE {
				
				@Override
				public final int computeValue(final int pixel1, final int value1, final int pixel2, final int value2) {
					return (int) sqrt(RGBA_SQUARED_EUCLIDEAN_DISTANCE.computeValue(pixel1, value1, pixel2, value2));
				}
				
			};
			
		}
		
	}
	
}
