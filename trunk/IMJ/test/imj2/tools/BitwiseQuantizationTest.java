package imj2.tools;

import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;

import java.awt.Color;
import java.io.Serializable;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import net.sourceforge.aprog.tools.MathTools.Statistics;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-04-09)
 */
public final class BitwiseQuantizationTest {
	
	@Test
	public final void test() {
		final TreeMap<double[], String> lines = new TreeMap<double[], String>(DoubleArrayComparator.INSTANCE);
		
		for (int qR0 = 0; qR0 <= 7; ++qR0) {
			final int qR = qR0;
			
			for (int qG0 = 0; qG0 <= 7; ++qG0) {
				final int qG = qG0;
				
				for (int qB0 = 0; qB0 <= 7; ++qB0) {
					final int qB = qB0;
					
					MultiThreadTools.getExecutor().submit(new Runnable() {
						
						@Override
						public final void run() {
							final int[] rgb = new int[3];
							final int[] qRGB = rgb.clone();
							final float[] xyz = new float[3];
							final float[] cielab = new float[3];
							final float[] qCIELAB = cielab.clone();
							final Statistics statistics = new Statistics();
							
							for (int color = 0; color <= 0x00FFFFFF; ++color) {
								rgbToRGB(color, rgb);
								
								rgbToXYZ(rgb, xyz);
								xyzToCIELAB(xyz, cielab);
								
								quantize(rgb, qR, qG, qB, qRGB);
								rgbToXYZ(qRGB, xyz);
								xyzToCIELAB(xyz, qCIELAB);
								
								statistics.addValue(distance2(cielab, qCIELAB));
							}
							
							final double[] key = { qR + qG + qB, statistics.getMean() };
							final String line = "qRGB: " + qR + " " + qG + " " + qB + " " + ((qR + qG + qB)) + " " + "error: " + statistics.getMinimum() + " <= " + statistics.getMean() + " ( " + sqrt(statistics.getVariance()) + ") <= " + statistics.getMaximum();
							
							synchronized (lines) {
								lines.put(key, line);
								System.out.println(line);
							}
						}
						
					});
				}
			}
		}
		
		for (int qH0 = 0; qH0 <= 7; ++qH0) {
			final int qH = qH0;
			
			for (int qS0 = 0; qS0 <= 7; ++qS0) {
				final int qS = qS0;
				
				for (int qV0 = 0; qV0 <= 7; ++qV0) {
					final int qV = qV0;
					
					MultiThreadTools.getExecutor().submit(new Runnable() {
						
						@Override
						public final void run() {
							final int[] rgb = new int[3];
							final int[] qRGB = rgb.clone();
							final float[] xyz = new float[3];
							final float[] cielab = new float[3];
							final float[] qCIELAB = cielab.clone();
							final int[] hsv = new int[3];
							final int[] qHSV = hsv.clone();
							final Statistics statistics = new Statistics();
							
							for (int color = 0; color <= 0x00FFFFFF; ++color) {
								rgbToRGB(color, rgb);
								
								rgbToXYZ(rgb, xyz);
								xyzToCIELAB(xyz, cielab);
								
								rgbToHSV(rgb, hsv);
								quantize(hsv, qH, qS, qV, qHSV);
								hsvToRGB(qHSV, qRGB);
								rgbToXYZ(qRGB, xyz);
								xyzToCIELAB(xyz, qCIELAB);
								
								statistics.addValue(distance2(cielab, qCIELAB));
							}
							
							final double[] key = { qH + qS + qV, statistics.getMean() };
							final String line = "qHSV: " + qH + " " + qS + " " + qV + " " + ((qH + qS + qV)) + " " + "error: " + statistics.getMinimum() + " <= " + statistics.getMean() + " ( " + sqrt(statistics.getVariance()) + ") <= " + statistics.getMaximum();
							
							synchronized (lines) {
								lines.put(key, line);
								System.out.println(line);
							}
						}
						
					});
				}
			}
		}
		
		shutdownAndWait(MultiThreadTools.getExecutor(), Long.MAX_VALUE);
		
		System.out.println();
		
		for (final String line : lines.values()) {
			System.out.println(line);
		}
	}
	
	public static final void shutdownAndWait(final ExecutorService executor, final long milliseconds) {
		executor.shutdown();
		
		try {
			executor.awaitTermination(milliseconds, TimeUnit.MILLISECONDS);
		} catch (final InterruptedException exception) {
			exception.printStackTrace();
		}
	}
	
	public static final int[] rgbToRGB(final int rgb, final int[] result) {
		result[0] = (rgb >> 16) & 0xFF;
		result[1] = (rgb >> 8) & 0xFF;
		result[2] = (rgb >> 0) & 0xFF;
		
		return result;
	}
	
	public static final int[] rgbToHSV(final int[] rgb, final int[] result) {
		final float[] hsv = new float[3];
		
		Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], hsv);
		
		result[0] = round(hsv[0] * 255F);
		result[1] = round(hsv[1] * 255F);
		result[2] = round(hsv[2] * 255F);
		
		return result;
	}
	
	public static final int[] hsvToRGB(final int[] hsv, final int[] result) {
		return rgbToRGB(Color.HSBtoRGB(hsv[0] / 255F, hsv[1] / 255F, hsv[2] / 255F), result);
	}
	
	public static final float[] rgbToXYZ(final int[] rgb, final float[] result) {
		// http://en.wikipedia.org/wiki/CIE_1931_color_space
		
		final float r = rgb[0] / 255F;
		final float g = rgb[1] / 255F;
		final float b = rgb[2] / 255F;
		final float b21 = 0.17697F;
		
		result[0] = (0.49F * r + 0.31F * g + 0.20F * b) / b21;
		result[1] = (b21 * r + 0.81240F * g + 0.01063F * b) / b21;
		result[2] = (0.00F * r + 0.01F * g + 0.99F * b) / b21;
		
		return result;
	}
	
	public static final int[] xyzToRGB(final float[] xyz, final int[] result) {
		// http://en.wikipedia.org/wiki/CIE_1931_color_space
		
		final float x = xyz[0];
		final float y = xyz[1];
		final float z = xyz[2];
		
		result[0] = round(255F * (0.41847F * x - 0.15866F * y - 0.082835F * z));
		result[1] = round(255F * (-0.091169F * x + 0.25243F * y + 0.015708F * z));
		result[2] = round(255F * (0.00092090F * x - 0.0025498F * y + 0.17860F * z));
		
		return result;
	}
	
	public static final float[] xyzToCIELAB(final float[] xyz, final float[] result) {
		// http://en.wikipedia.org/wiki/Illuminant_D65
		
		final float d65X = 0.95047F;
		final float d65Y = 1.0000F;
		final float d65Z = 1.08883F;
		
		// http://en.wikipedia.org/wiki/Lab_color_space
		
		final float fX = f(xyz[0] / d65X);
		final float fY = f(xyz[1] / d65Y);
		final float fZ = f(xyz[2] / d65Z);
		
		result[0] = 116F * fY - 16F;
		result[1] = 500F * (fX - fY);
		result[2] = 200F * (fY - fZ);
		
		return result;
	}
	
	public static final float[] cielabToXYZ(final float[] cielab, final float[] result) {
		// http://en.wikipedia.org/wiki/Illuminant_D65
		
		final float d65X = 0.95047F;
		final float d65Y = 1.0000F;
		final float d65Z = 1.08883F;
		
		// http://en.wikipedia.org/wiki/Lab_color_space
		
		final float lStar = cielab[0];
		final float aStar = cielab[1];
		final float bStar = cielab[2];
		final float c = (lStar + 16F) / 116F;
		
		result[0] = d65X * fInv(c + aStar / 500F);
		result[1] = d65Y * fInv(c);
		result[2] = d65Z * fInv(c - bStar / 200F);
		
		return result;
	}
	
	public static final float f(final float t) {
		return cube(6F / 29F) < t ? (float) pow(t, 1.0 / 3.0) : square(29F / 6F) * t / 3F + 4F / 29F;
	}
	
	public static final float fInv(final float t) {
		return 6F / 29F < t ? cube(t) : 3F * square(6F / 29F) * (t - 4F / 29F);
	}
	
	public static final float square(final float value) {
		return value * value;
	}
	
	public static final float cube(final float value) {
		return value * value * value;
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
	
	public static final double distance2(final float[] abc1, final float[] abc2) {
		final int n = abc1.length;
		double sum = 0.0;
		
		for (int i = 0; i < n; ++i) {
			sum += square(abc1[i] - abc2[i]);
		}
		
		return sqrt(sum);
	}
	
	/**
	 * @author codistmonk (creation 2014-04-10)
	 */
	public static final class DoubleArrayComparator implements Serializable, Comparator<double[]> {
		
		@Override
		public final int compare(final double[] array1, final double[] array2) {
			final int n1 = array1.length;
			final int n2 = array2.length;
			final int n = Math.min(n1, n2);
			
			for (int i = 0; i < n; ++i) {
				final int comparison = Double.compare(array1[i], array2[i]);
				
				if (comparison != 0) {
					return comparison;
				}
			}
			
			return n1 - n2;
		}
		
		/**
		 * {@value}.
		 */
		private static final long serialVersionUID = -88586465954519984L;
		
		public static final DoubleArrayComparator INSTANCE = new DoubleArrayComparator();
		
	}
	
}
