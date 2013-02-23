package imj.apps.modules;

import static imj.IMJTools.alpha;
import static imj.IMJTools.argb;
import static imj.IMJTools.blue;
import static imj.IMJTools.brightness;
import static imj.IMJTools.hue;
import static imj.IMJTools.red;
import static imj.IMJTools.saturation;
import static java.lang.Integer.parseInt;
import static java.util.Locale.ENGLISH;
import imj.apps.modules.FilteredImage.Filter;

import java.awt.Color;

import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class SimpleSieve extends Sieve {
	
	private Feature feature;
	
	private int minimum;
	
	private int maximum;
	
	public SimpleSieve(final Context context) {
		super(context);
		
		this.getParameters().put("feature", "brightness");
		this.getParameters().put("minimum", "0");
		this.getParameters().put("maximum", "255");
	}
	
	@Override
	public final boolean accept(final int index, final int value) {
		if (this.feature == null) {
			return true;
		}
		
		if (this.maximum < this.minimum) {
			return false;
		}
		
		final int featureValue = this.feature.getNewValue(index, value);
		
		return this.minimum <= featureValue && featureValue <= this.maximum;
	}
	
	@Override
	public final void initialize() {
		this.feature = Feature.valueOf(this.getParameters().get("feature").toUpperCase(ENGLISH));
		this.minimum = parseInt(this.getParameters().get("minimum"));
		this.maximum = parseInt(this.getParameters().get("maximum"));
	}
	
	/**
	 * @author codistmonk (creation 2013-02-18)
	 */
	public static enum Feature implements Filter {
		
		RED {
			
			@Override
			public final int getNewValue(final int index, final int oldValue) {
				return red(oldValue);
			}
			
		}, GREEN {
			
			@Override
			public final int getNewValue(final int index, final int oldValue) {
				return red(oldValue);
			}
			
		}, BLUE {
			
			@Override
			public final int getNewValue(final int index, final int oldValue) {
				return blue(oldValue);
			}
			
		}, ALPHA {
			
			@Override
			public final int getNewValue(final int index, final int oldValue) {
				return alpha(oldValue);
			}
			
		}, BRIGHTNESS {
			
			@Override
			public final int getNewValue(final int index, final int oldValue) {
				return brightness(oldValue);
			}
			
		}, SATURATION {
			
			@Override
			public final int getNewValue(final int index, final int oldValue) {
				return saturation(oldValue);
			}
			
		}, HUE {
			
			@Override
			public final int getNewValue(final int index, final int oldValue) {
				return hue(oldValue);
			}
			
		};
		
		static {
			final float[] hsb = new float[3];
			
			for (int r = 0; r < 256; r += 2) {
				for (int g = 0; g < 256; g += 2) {
					for (int b = 0; b < 256; b += 2) {
						Color.RGBtoHSB(r, g, b, hsb);
						
						if (HUE.getNewValue(0, argb(255, r, g, b)) != (int) (hsb[0] * 255F)) {
							throw new RuntimeException();
						}
					}
				}
			}
		}
		
	}
	
}
