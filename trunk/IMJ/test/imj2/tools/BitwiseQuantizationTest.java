package imj2.tools;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.MathTools.square;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-04-09)
 */
public final class BitwiseQuantizationTest {
	
	@Test
	public final void test() {
		fail("Not yet implemented");
	}
	
	public static final int[] rgbToHSV(final int[] rgb, final int[] result) {
		return result;
	}
	
	public static final int[] hsvToRGB(final int[] hsv, final int[] result) {
		return result;
	}
	
	public static final int[] rgbToXYZ(final int[] rgb, final int[] result) {
		return result;
	}
	
	public static final int[] quantize(final int[] abc, final int q, final int[] result) {
		return quantize(abc, q, q, q, result);
	}
	
	public static final int[] quantize(final int[] abc, final int qA, final int qB, final int qC, final int[] result) {
		result[0] = abc[0] & ((~0) << qA);
		result[1] = abc[1] & ((~0) << qB);
		result[2] = abc[2] & ((~0) << qC);
		
		return result;
	}
	
	public static final double distance0(final int[] abc1, final int[] abc2) {
		final int n = abc1.length;
		double result = 0.0;
		
		for (int i = 0; i < n; ++i) {
			result = max(result, abs(abc1[i] - abc2[i]));
		}
		
		return result;
	}
	
	public static final double distance1(final int[] abc1, final int[] abc2) {
		final int n = abc1.length;
		double result = 0.0;
		
		for (int i = 0; i < n; ++i) {
			result += abs(abc1[i] - abc2[i]);
		}
		
		return result;
	}
	
	public static final double distance2(final int[] abc1, final int[] abc2) {
		final int n = abc1.length;
		double sum = 0.0;
		
		for (int i = 0; i < n; ++i) {
			sum += square(abc1[i] - abc2[i]);
		}
		
		return sqrt(sum);
	}
	
}
