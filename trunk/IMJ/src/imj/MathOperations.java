package imj;

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
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			result.setValue(pixel, operator.evaluate(left.getValue(pixel), right.getValue(pixel)));
		}
		
		return result;
	}
	
	public static final Image compute(final Image left, final BinaryOperator operator, final Image right) {
		return compute(left, operator, right, new ImageOfInts(left.getRowCount(), left.getColumnCount()));
	}
	
	public static final Image compute(final UnaryOperator operator, final Image image, final Image result) {
		final int pixelCount = result.getRowCount() * result.getColumnCount();
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			result.setValue(pixel, operator.evaluate(image.getValue(pixel)));
		}
		
		return result;
	}
	
	public static final Image compute(final UnaryOperator operator, final Image image) {
		return compute(operator, image, new ImageOfInts(image.getRowCount(), image.getColumnCount()));
	}
	
	/**
	 * @author codistmonk (creation 2013-01-27)
	 */
	public static enum UnaryOperator {
		
		NEGATE {
			
			@Override
			public final int evaluate(final int value) {
				return -value;
			}
			
		}, COMPLEMENT {
			
			@Override
			public final int evaluate(final int value) {
				return ~value;
			}
			
		}, ABSOLUTE_VALUE {
			
			@Override
			public final int evaluate(final int value) {
				return abs(value);
			}
			
		}, SQUARE {
			
			@Override
			public final int evaluate(final int value) {
				return value * value;
			}
			
		}, CUBE {
			
			@Override
			public final int evaluate(final int value) {
				return value * value * value;
			}
			
		}, SQUARE_ROOT {
			
			@Override
			public final int evaluate(final int value) {
				return (int) sqrt(value);
			}
			
		}, LOG_2 {
			
			@Override
			public final int evaluate(final int value) {
				return (int) (log(value) / LOG2);
			}
			
		}, LOG_10 {
			
			@Override
			public final int evaluate(final int value) {
				return (int) (log10(value));
			}
			
		}, LOG {
			
			@Override
			public final int evaluate(final int value) {
				return (int) (log(value));
			}
			
		};
		
		public static final double LOG2 = log(2.0);
		
		public abstract int evaluate(int value);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-01-27)
	 */
	public static enum BinaryOperator {
		
		PLUS {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return left + right;
			}
			
		}, MINUS {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return left - right;
			}
			
		}, TIMES {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return left * right;
			}
			
		}, DIVIDED_BY {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return left / right;
			}
			
		}, MODULO {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return left % right;
			}
			
		}, POWER {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return (int) pow(left, right);
			}
			
		}, ROOT {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return (int) pow(left, 1.0 / (double) right);
			}
			
		}, MIN {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return min(left, right);
			}
			
		}, MAX {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return max(left, right);
			}
			
		}, AND {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return left & right;
			}
			
		}, OR {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return left | right;
			}
			
		}, XOR {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return left ^ right;
			}
			
		}, IS_LESS_THAN {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return left < right ? 1 : 0;
			}
			
		}, IS_LESS_THAN_OR_EQUAL_TO {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return left <= right ? 1 : 0;
			}
			
		}, IS_GREATER_THAN {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return left > right ? 1 : 0;
			}
			
		}, IS_GREATER_THAN_OR_EQUAL_TO {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return left >= right ? 1 : 0;
			}
			
		}, IS_EQUAL_TO {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return left == right ? 1 : 0;
			}
			
		}, IS_NOT_EQUAL_TO {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return left != right ? 1 : 0;
			}
			
		}, SHIFTED_LEFT_BY {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return left << right;
			}
			
		}, SHIFTED_RIGHT_BY {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return left >> right;
			}
			
		}, UNSIGNED_SHIFTED_RIGHT_BY {
			
			@Override
			public final int evaluate(final int left, final int right) {
				return left >>> right;
			}
			
		};
		
		public abstract int evaluate(int left, int right);
		
	}
	
}
