package imj3.draft;

import static java.lang.Math.pow;
import static java.lang.Math.round;

import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2014-04-24)
 */
public final class ColorTools2 {
	
	private ColorTools2() {
		throw new IllegalInstantiationException();
	}
	
	public static final float[] floats(final float... result) {
		return result;
	}
	
	public static final int[] xyzToRGB(final float[] xyz) {
		return xyzToRGB(xyz, new int[3]);
	}
	
	public static final int[] xyzToRGB(final float[] xyz, final int[] rgb) {
		// http://www.easyrgb.com/index.php?X=MATH&H=01#text1
		
		final double x = xyz[0] / 100.0;        //xyz[0] from 0 to  95.047      (Observer = 2°, Illuminant = D65)
		final double y = xyz[1] / 100.0;        //xyz[1] from 0 to 100.000
		final double z = xyz[2] / 100.0;        //xyz[2] from 0 to 108.883

		double r = x *  3.2406 + y * -1.5372F + z * -0.4986;
		double g = x * -0.9689 + y *  1.8758F + z *  0.0415;
		double b = x *  0.0557 + y * -0.2040F + z *  1.0570;

		if (r > 0.0031308) {
			r = 1.055 * pow(r, 1.0 / 2.4) - 0.055;
		} else {
			r = 12.92 * r;
		}
		
		if ( g > 0.0031308 ) {
			g = 1.055 * pow(g, 1.0 / 2.4) - 0.055;
		} else {
			g = 12.92 * g;
		}
		
		if (b > 0.0031308) {
			b = 1.055 * pow(b, 1.0 / 2.4) - 0.055;
		} else {
			b = 12.92 * b;
		}

		rgb[0] = (int) round(r * 255.0);
		rgb[1] = (int) round(g * 255.0);
		rgb[2] = (int) round(b * 255.0);
				
		return rgb;
	}
	
	public static final float[] rgbToXYZ(final int[] rgb) {
		return rgbToXYZ(rgb, new float[3]);
	}
	
	public static final float[] rgbToXYZ(final int[] rgb, final float[] xyz) {
		// http://www.easyrgb.com/index.php?X=MATH&H=02#text2
		
		double r = rgb[0] / 255.0;        //R from 0 to 255
		double g = rgb[1] / 255.0;        //G from 0 to 255
		double b = rgb[2] / 255.0;        //B from 0 to 255

		if (r > 0.04045) {
			r = pow((r + 0.055) / 1.055, 2.4);
		} else {
			r = r / 12.92;
		}
		
		if (g > 0.04045) {
			g = pow((g + 0.055) / 1.055, 2.4);
		} else {
			g = g / 12.92;
		}
		
		if (b > 0.04045) {
			b = pow((b + 0.055) / 1.055, 2.4);
		} else {
			b = b / 12.92;
		}
		
		r = r * 100.0;
		g = g * 100.0;
		b = b * 100.0;
		
		//Observer = 2°, Illuminant = D65
		xyz[0] = (float) (r * 0.4124 + g * 0.3576 + b * 0.1805);
		xyz[1] = (float) (r * 0.2126 + g * 0.7152 + b * 0.0722);
		xyz[2] = (float) (r * 0.0193 + g * 0.1192 + b * 0.9505);
		
		return xyz;
	}
	
	public static final float[] xyzToCIELAB(final float[] abc) {
		return xyzToCIELAB(abc, abc);
	}
	
	public static final float[] xyzToCIELAB(final float[] xyz, final float[] cielab) {
		return xyzToCIELAB(xyz, Illuminant.D65.getX2Y2Z2(), cielab);
	}
	
	public static final float[] xyzToCIELAB(final float[] xyz, final float[] referenceXYZ, final float[] cielab) {
		// http://www.easyrgb.com/index.php?X=MATH&H=07#text7
		
		double x = xyz[0] / referenceXYZ[0];
		double y = xyz[1] / referenceXYZ[1];
		double z = xyz[2] / referenceXYZ[2];
		
		if (x > 0.008856) {
			x = pow(x, 1.0 / 3.0);
		} else {
			x = 7.787 * x + 16.0 / 116.0;
		}
		
		if ( y > 0.008856 ) {
			y = pow(y, 1.0 / 3.0);
		} else {
			y = 7.787 * y + 16.0 / 116.0;
		}
		
		if (z > 0.008856) {
			z = pow(z, 1.0 / 3.0);
		} else {
			z = 7.787 * z + 16.0 / 116.0;
		}
		
		cielab[0] = (float) (116.0 * y - 16.0); // L*
		cielab[1] = (float) (500.0 * (x - y)); // a*
		cielab[2] = (float) (200.0 * (y - z)); // b*
		
		return cielab;
	}
	
	public static final float[] cielabToXYZ(final float[] abc) {
		return cielabToXYZ(abc, abc);
	}
	
	public static final float[] cielabToXYZ(final float[] cielab, final float[] xyz) {
		return cielabToXYZ(cielab, Illuminant.D65.getX2Y2Z2(), xyz);
	}
	
	public static final float[] cielabToXYZ(final float[] cielab, final float[] referenceXYZ, final float[] xyz) {
		// http://www.easyrgb.com/index.php?X=MATH&H=08#text8
		
		double y = (cielab[0] + 16.0) / 116.0;
		double x = cielab[1] / 500.0 + y;
		double z = y - cielab[2] / 200.0;
		
		if (cube(y) > 0.008856) {
			y = cube(y);
		} else {
			y = (y - 16.0 / 116.0) / 7.787;
		}
		
		if (cube(x) > 0.008856) {
			x = cube(x);
		} else {
			x = (x - 16.0 / 116.0) / 7.787;
		}
		
		if (cube(z) > 0.008856) {
			z = cube(z);
		} else {
			z = (z - 16.0 / 116.0) / 7.787;
		}
		
		xyz[0] = (float) (referenceXYZ[0] * x);
		xyz[1] = (float) (referenceXYZ[1] * y);
		xyz[2] = (float) (referenceXYZ[2] * z);
		
		return xyz;
	}
	
	public static final float[] rgbToCIELAB(final int[] rgb) {
		return rgbToCIELAB(rgb, new float[3]);
	}
	
	public static final float[] rgbToCIELAB(final int[] rgb, final float[] cielab) {
		return rgbToCIELAB(rgb, Illuminant.D65.getX2Y2Z2(), cielab);
	}
	
	public static final float[] rgbToCIELAB(final int[] rgb, final float[] referenceXYZ, final float[] cielab) {
		return xyzToCIELAB(rgbToXYZ(rgb, cielab), referenceXYZ, cielab);
	}
	
	public static final int[] cielabToRGB(final float[] cielab) {
		return cielabToRGB(cielab, new int[3]);
	}
	
	public static final int[] cielabToRGB(final float[] cielab, final int[] rgb) {
		return cielabToRGB(cielab, Illuminant.D65.getX2Y2Z2(), rgb);
	}
	
	public static final int[] cielabToRGB(final float[] cielab, final float[] referenceXYZ, final int[] rgb) {
		return xyzToRGB(cielabToXYZ(cielab, referenceXYZ, new float[3]), rgb);
	}
	
	public static final double cube(final double value) {
		return value * value * value;
	}
	
	/**
	 * @author codistmonk (creation 2014-04-24)
	 */
	public static enum Illuminant {
		
		A {
			
			@Override
			public final float[] getX2Y2Z2() {
				return floats(109.850F, 100F, 35.585F);
			}
			
			@Override
			public final float[] getX10Y10Z10() {
				return floats(111.144F, 100F, 35.200F);
			}
			
		}, C {
			
			@Override
			public final float[] getX2Y2Z2() {
				return floats(98.074F, 100F, 118.232F);
			}
			
			@Override
			public final float[] getX10Y10Z10() {
				return floats(97.285F, 100F, 116.145F);
			}
			
		}, D50 {
			
			@Override
			public final float[] getX2Y2Z2() {
				return floats(96.422F, 100F, 82.521F);
			}
			
			@Override
			public final float[] getX10Y10Z10() {
				return floats(96.720F, 100F, 81.427F);
			}
			
		}, D55 {
			
			@Override
			public final float[] getX2Y2Z2() {
				return floats(95.682F, 100F, 92.149F);
			}
			
			@Override
			public final float[] getX10Y10Z10() {
				return floats(95.799F, 100F, 90.926F);
			}
			
		}, D65 {
			
			@Override
			public final float[] getX2Y2Z2() {
				return floats(95.047F, 100F, 108.883F);
			}
			
			@Override
			public final float[] getX10Y10Z10() {
				return floats(94.811F, 100F, 107.304F);
			}
			
		}, D75 {
			
			@Override
			public final float[] getX2Y2Z2() {
				return floats(94.972F, 100F, 122.638F);
			}
			
			@Override
			public final float[] getX10Y10Z10() {
				return floats(94.416F, 100F, 120.641F);
			}
			
		}, F2 {
			
			@Override
			public final float[] getX2Y2Z2() {
				return floats(99.187F, 100F, 67.395F);
			}
			
			@Override
			public final float[] getX10Y10Z10() {
				return floats(103.280F, 100F, 69.026F);
			}
			
		}, F7 {
			
			@Override
			public final float[] getX2Y2Z2() {
				return floats(95.044F, 100F, 108.755F);
			}
			
			@Override
			public final float[] getX10Y10Z10() {
				return floats(95.792F, 100F, 107.687F);
			}
			
		}, F11 {
			
			@Override
			public final float[] getX2Y2Z2() {
				return floats(100.966F, 100F, 64.370F);
			}
			
			@Override
			public final float[] getX10Y10Z10() {
				return floats(103.866F, 100F, 65.627F);
			}
			
		}, ICC {
			
			@Override
			public final float[] getX2Y2Z2() {
				return floats(96.42F, 100F, 82.49F);
			}
			
			@Override
			public final float[] getX10Y10Z10() {
				// TODO correct?
				return this.getX2Y2Z2();
			}
			
		};
		
		public abstract float[] getX2Y2Z2();
		
		public abstract float[] getX10Y10Z10();
		
	}
	
}
