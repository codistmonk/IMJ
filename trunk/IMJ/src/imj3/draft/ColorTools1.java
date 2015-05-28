package imj3.draft;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;

import java.awt.Color;

import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2014-04-24)
 */
public final class ColorTools1 {
	
	private ColorTools1() {
		throw new IllegalInstantiationException();
	}
	
	public static final int[] ints(final int... result) {
		return result;
	}
	
	public static final int packRGB(final int[] rgb) {
		return 0xFF000000 | ((rgb[0] & 0xFF) << 16) | ((rgb[1] & 0xFF) << 8) |((rgb[2] & 0xFF) << 0);
	}
	
	public static final int packCIELAB(final float[] cielab) {
		return 0xFF000000 | (round((cielab[0] % 100F) * 2.55F) << 16) | (round((cielab[1] % 100F) * 2.55F) << 8) | (round((cielab[2] % 100F) * 2.55F) << 0);
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
	
	public static final float[] rgbToCIELAB(final int[] rgb) {
		return rgbToCIELAB(rgb, new float[3]);
	}
	
	public static final float[] rgbToCIELAB(final int[] rgb, final float[] result) {
		return xyzToCIELAB(rgbToXYZ(rgb, result));
	}
	
	public static final int[] cielabToRGB(final float[] cielab) {
		return cielabToRGB(cielab, new int[3]);
	}
	
	public static final int[] cielabToRGB(final float[] cielab, final int[] result) {
		return xyzToRGB(cielabToXYZ(cielab, new float[3]), result);
	}
	
	public static final float[] rgbToXYZ(final int[] rgb) {
		return rgbToXYZ(rgb, new float[3]);
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
	
	public static final int[] xyzToRGB(final float[] xyz) {
		return xyzToRGB(xyz, new int[3]);
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
	
	public static final float[] xyzToCIELAB(final float[] abc) {
		return xyzToCIELAB(abc, abc);
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
	
	public static final float[] cielabToXYZ(final float[] abc) {
		return cielabToXYZ(abc, abc);
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
	
	public static final int distance1(final int[] abc1, final int[] abc2) {
		final int n = abc1.length;
		int result = 0;
		
		for (int i = 0; i < n; ++i) {
			result += abs(abc1[i] - abc2[i]);
		}
		
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
	
	public static final int min(final int... values) {
		int result = Integer.MAX_VALUE;
		
		for (final int value : values) {
			if (value < result) {
				result = value;
			}
		}
		
		return result;
	}
	
	public static final int max(final int... values) {
		int result = Integer.MIN_VALUE;
		
		for (final int value : values) {
			if (result < value) {
				result = value;
			}
		}
		
		return result;
	}
	
}
