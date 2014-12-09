package ojalgotest;

import static java.lang.Math.min;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.ojalgo.access.Access2D.Builder;
import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.PrimitiveMatrix;

import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2014-12-08)
 */
public final class SeparatingHyperplane {
	
	private SeparatingHyperplane() {
		throw new IllegalInstantiationException();
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		final int dimension = 1;
		final int homogeneousDimension = dimension + 1;
		final float[] data = {
				0F, 1F
		};
		final int n = data.length / dimension;
		final int[] expectedClasses = {
				0, 1
		};
		
		// TODO
	}
	
	public static final float[] square(final float[] data, final int dimension, final Function<Integer, Boolean> filter) {
		final int n = data.length / dimension;
		final float[] result = new float[dimension * dimension];
		
		// data[i * n + j] = M(i, j)
		// (A B)(i, j) = Sum(A(i, k) * B(k, j), k=1..n)
		// (M M')(i, j) = Sum(M(i, k) * M'(k, j), k=1..n)
		//              = Sum(M(i, k) * M(j, k), k=1..n)
		
		for (int k = 0; k < n; ++k) {
			if (filter.apply(k)) {
				for (int i = 0; i < dimension; ++i) {
					for (int j = 0; j < dimension; ++j) {
						result[i * dimension + j] = data[i * n + k] * data[j * n + k];
					}
				}
			}
		}
		
		return result;
	}
	
	public static final double[] toArray(final BasicMatrix<Double> columnVector) {
		if (columnVector.countColumns() != 1L) {
			throw new IllegalArgumentException();
		}
		
		final int n = (int) columnVector.countRows();
		final double[] result = new double[n];
		
		for (int i = 0; i < n; ++i) {
			result[i] = columnVector.doubleValue(i);
		}
		
		return result;
	}
	
	public static final BasicMatrix<Double> matrix(final int columnCount, final double... values) {
		final Builder<PrimitiveMatrix> builder = PrimitiveMatrix.getBuilder(values.length / columnCount, columnCount);
		
		for (int i = 0; i < values.length; ++i) {
			builder.set(i, values[i]);
		}
		
		return builder.build();
	}
	
	public static final BasicMatrix<Double> columnVector(final double... values) {
		final Builder<PrimitiveMatrix> builder = PrimitiveMatrix.getBuilder(values.length);
		
		setColumn(builder, 0, values);
		
		return builder.build();
	}
	
	public static final void setColumn(final Builder<?> builder, final int columnIndex, final double... values) {
		final int n = min((int) builder.countRows(), values.length);
		
		for (int i = 0; i < n; ++i) {
			builder.set(i, columnIndex, values[i]);
		}
	}
	
}
