package imj.apps.modules;

import static java.lang.Double.parseDouble;
import static java.lang.Math.exp;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.MathTools.square;

import imj.apps.modules.FilteredImage.LinearFilter;

import java.util.Arrays;
import java.util.Locale;

import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class LinearViewFilter extends ViewFilter.FromFilter {
	
	public LinearViewFilter(final Context context) {
		super(context);
		
		this.getParameters().put("kernel", "gaussian 1");
	}
	
	@Override
	protected final void doInitialize() {
		super.doInitialize();
		
		final String[] kernelParameters = this.getParameters().get("kernel").trim().split("\\s+");
		final String kernelType = kernelParameters[0].toLowerCase(Locale.ENGLISH);
		final int[] structuringElement = this.parseStructuringElement();
		final double[] coefficients;
		
		if ("gaussian".equals(kernelType)) {
			final double sigma = parseDouble(kernelParameters[1]);
			coefficients = gaussian(structuringElement, sigma);
		} else if ("mean".equals(kernelType)) {
			coefficients = mean(structuringElement);
		} else {
			throw new IllegalArgumentException("Invalid kernel type: " + kernelType);
		}
		
		this.setFilter(new LinearFilter(structuringElement, coefficients));
	}
	
	public static final double SQRT_2_PI = sqrt(2.0 * Math.PI);
	
	public static final double[] gaussian(final int[] structuringElement, final double sigma) {
		final double kp = sigma * SQRT_2_PI;
		final double ks = - 2.0 * square(sigma);
		final int n = structuringElement.length / 2;
		final double[] result = new double[n];
		double sum = 0.0;
		
		for (int i = 0; i < n; ++i) {
			final double dy = structuringElement[i * 2 + 0];
			final double dx = structuringElement[i * 2 + 1];
			final double d2 = square(dx) + square(dy);
			
			result[i] = exp(d2 / ks) / kp;
			sum += result[i];
		}
		
		if (sum != 0.0) {
			for (int i = 0; i < n; ++i) {
				result[i] /= sum;
			}
		}
		
		return result;
	}
	
	public static final double[] mean(final int[] structuringElement) {
		final int n = structuringElement.length / 2;
		final double[] result = new double[n];
		
		Arrays.fill(result, 1.0 / (double) n);
		
		return result;
	}
	
}
