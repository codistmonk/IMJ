package imj;

import static imj.IMJTools.channelValue;
import static java.lang.Math.abs;
import static java.lang.Math.log;
import static java.lang.Math.log10;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-01-27)
 */
public final class MathOperations {
	
	private MathOperations() {
		throw new IllegalInstantiationException();
	}
	
	public static final Image compute(final Image left, final BinaryOperator operator, final Image right, final Image result) {
		final int pixelCount = result.getRowCount() * result.getColumnCount();
		final int channelCount = left.getChannelCount();
		
		if (1 < channelCount) {
			final int alpha = channelCount < 4 ? 0xFF000000 : 0;
			
			for (int pixel = 0; pixel < pixelCount; ++pixel) {
				int resultPixelValue = 0;
				
				for (int channel = 0; channel < channelCount; ++channel) {
					final int leftChannelValue = channelValue(left.getValue(pixel), channel);
					final int rightChannelValue = channelValue(right.getValue(pixel), channel);
					
					resultPixelValue = (resultPixelValue << 8) |
							min(255, operator.evaluate(pixel, leftChannelValue, rightChannelValue));
				}
				
				result.setValue(pixel, resultPixelValue | alpha);
			}
		} else {
			for (int pixel = 0; pixel < pixelCount; ++pixel) {
				result.setValue(pixel, operator.evaluate(pixel, left.getValue(pixel), right.getValue(pixel)));
			}
		}
		
		return result;
	}
	
	public static final Image compute(final Image left, final BinaryOperator operator, final Image right) {
		return compute(left, operator, right, new ImageOfInts(left.getRowCount(), left.getColumnCount(), left.getChannelCount()));
	}
	
	public static final Image compute(final UnaryOperator operator, final Image image, final Image result) {
		final int pixelCount = result.getRowCount() * result.getColumnCount();
		final int channelCount = image.getChannelCount();
		
		if (1 < channelCount) {
			final int alpha = channelCount < 4 ? 0xFF000000 : 0;
			
			for (int pixel = 0; pixel < pixelCount; ++pixel) {
				int resultPixelValue = 0;
				
				for (int channel = channelCount - 1; 0 <= channel; --channel) {
					final int channelValue = channelValue(image.getValue(pixel), channel);
					
					resultPixelValue = (resultPixelValue << 8) | min(255, operator.evaluate(pixel, channelValue));
				}
				
				result.setValue(pixel, resultPixelValue | alpha);
			}
		} else {
			for (int pixel = 0; pixel < pixelCount; ++pixel) {
				result.setValue(pixel, operator.evaluate(pixel, image.getValue(pixel)));
			}
		}
		
		
		return result;
	}
	
	public static final Image compute(final UnaryOperator operator, final Image image) {
		return compute(operator, image, new ImageOfInts(image.getRowCount(), image.getColumnCount(), image.getChannelCount()));
	}
	
	/**
	 * @author codistmonk (creation 2013-01-28)
	 */
	public static abstract interface UnaryOperator {
		
		public abstract int evaluate(int pixel, int value);
		
		/**
		 * @author codistmonk (creation 2013-01-27)
		 */
		public static enum Predefined implements UnaryOperator {
			
			NEGATE {
				
				@Override
				public final int evaluate(final int pixel, final int value) {
					return -value;
				}
				
			}, COMPLEMENT {
				
				@Override
				public final int evaluate(final int pixel, final int value) {
					return ~value;
				}
				
			}, ABSOLUTE_VALUE {
				
				@Override
				public final int evaluate(final int pixel, final int value) {
					return abs(value);
				}
				
			}, SQUARE {
				
				@Override
				public final int evaluate(final int pixel, final int value) {
					return value * value;
				}
				
			}, CUBE {
				
				@Override
				public final int evaluate(final int pixel, final int value) {
					return value * value * value;
				}
				
			}, SQUARE_ROOT {
				
				@Override
				public final int evaluate(final int pixel, final int value) {
					return (int) sqrt(value);
				}
				
			}, LOG_2 {
				
				@Override
				public final int evaluate(final int pixel, final int value) {
					return (int) (log(value) / LOG2);
				}
				
			}, LOG_10 {
				
				@Override
				public final int evaluate(final int pixel, final int value) {
					return (int) (log10(value));
				}
				
			}, LOG {
				
				@Override
				public final int evaluate(final int pixel, final int value) {
					return (int) (log(value));
				}
				
			};
			
			public static final double LOG2 = log(2.0);
			
		}
		
	}
	
	
	/**
	 * @author codistmonk (creation 2013-01-28)
	 */
	public static abstract interface BinaryOperator {
		
		public abstract int evaluate(int pixel, int left, int right);
		
		public abstract UnaryOperator bindLeft(int pixel, int left);
		
		public abstract UnaryOperator bindRight(int pixel, int right);
		
		/**
		 * @author codistmonk (creation 2013-01-27)
		 */
		public static enum Predefined implements BinaryOperator {
			
			PLUS {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return left + right;
				}
				
			}, MINUS {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return left - right;
				}
				
			}, TIMES {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return left * right;
				}
				
			}, DIVIDED_BY {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return left / right;
				}
				
			}, MODULO {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return left % right;
				}
				
			}, POWER {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return (int) pow(left, right);
				}
				
			}, ROOT {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return (int) pow(left, 1.0 / (double) right);
				}
				
			}, MIN {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return min(left, right);
				}
				
			}, MAX {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return max(left, right);
				}
				
			}, AND {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return left & right;
				}
				
			}, OR {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return left | right;
				}
				
			}, XOR {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return left ^ right;
				}
				
			}, IS_LESS_THAN {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return left < right ? 1 : 0;
				}
				
			}, IS_LESS_THAN_OR_EQUAL_TO {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return left <= right ? 1 : 0;
				}
				
			}, IS_GREATER_THAN {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return left > right ? 1 : 0;
				}
				
			}, IS_GREATER_THAN_OR_EQUAL_TO {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return left >= right ? 1 : 0;
				}
				
			}, IS_EQUAL_TO {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return left == right ? 1 : 0;
				}
				
			}, IS_NOT_EQUAL_TO {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return left != right ? 1 : 0;
				}
				
			}, SHIFTED_LEFT_BY {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return left << right;
				}
				
			}, SHIFTED_RIGHT_BY {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return left >> right;
				}
				
			}, UNSIGNED_SHIFTED_RIGHT_BY {
				
				@Override
				public final int evaluate(final int pixel, final int left, final int right) {
					return left >>> right;
				}
				
			};
			
			@Override
			public final UnaryOperator bindLeft(final int pixel, final int left) {
				return new UnaryOperator() {
					
					@Override
					public final int evaluate(final int pixel, final int value) {
						return BinaryOperator.Predefined.this.evaluate(pixel, left, value);
					}
					
				};
			}
			
			@Override
			public final UnaryOperator bindRight(final int pixel, final int right) {
				return new UnaryOperator() {
					
					@Override
					public final int evaluate(final int pixel, final int value) {
						return BinaryOperator.Predefined.this.evaluate(pixel, value, right);
					}
					
				};
			}
			
		}
		
	}
	
}
