package imj2.tools;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;
import static net.sourceforge.aprog.tools.MathTools.square;
import static net.sourceforge.aprog.tools.Tools.debugPrint;
import static org.junit.Assert.*;

import java.awt.Color;
import java.util.Arrays;

import net.sourceforge.aprog.tools.MathTools;
import net.sourceforge.aprog.tools.MathTools.Statistics;
import net.sourceforge.aprog.tools.Tools;

import org.junit.Test;

/**
 * @author codistmonk (creation 2014-04-09)
 */
public final class BitwiseQuantizationTest {
	
	@Test
	public final void test() {
		final int[] rgb = new int[3];
		final int[] qRGB = rgb.clone();
		final float[] xyz = new float[3];
		final float[] cielab = new float[3];
		final float[] qCIELAB = cielab.clone();
		
		for (int q = 0; q <= 7; ++q) {
			final Statistics statistics = new Statistics();
			
			for (int color = 0; color <= 0x00FFFFFF; ++color) {
				rgbToRGB(color, rgb);
				
				rgbToXYZ(rgb, xyz);
				xyzToCIELAB(xyz, cielab);
				
				quantize(rgb, q, qRGB);
				rgbToXYZ(qRGB, xyz);
				xyzToCIELAB(xyz, qCIELAB);
				
				statistics.addValue(distance2(cielab, qCIELAB));
			}
			
			debugPrint("q:", q, "error:", statistics.getMinimum(), "<=", statistics.getMean(), "(" + sqrt(statistics.getVariance()) + ")", "<=", statistics.getMaximum());
		}
		
		final int[] hsv = new int[3];
		final int[] qHSV = hsv.clone();
		
		for (int qH = 0; qH <= 7; ++qH) {
			for (int qS = 0; qS <= 7; ++qS) {
				for (int qV = 0; qV <= 7; ++qV) {
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
					
					debugPrint("qHSV:", qH, qS, qV, "error:", statistics.getMinimum(), "<=", statistics.getMean(), "(" + sqrt(statistics.getVariance()) + ")", "<=", statistics.getMaximum());
				}
			}
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
	
}
