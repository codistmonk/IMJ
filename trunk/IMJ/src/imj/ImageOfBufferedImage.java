package imj;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.lang.reflect.Array;

/**
 * @author codistmonk (creation 2013-01-23)
 */
public final class ImageOfBufferedImage extends Image.Abstract {
	
	private final char[] data;
	
	public ImageOfBufferedImage(final BufferedImage source, final Feature feature) {
		super(source.getHeight(), source.getWidth(), feature.getResultChannelCount());
		this.data = new char[this.getRowCount() * this.getColumnCount()];
		final boolean hasAlpha = source.getColorModel().hasAlpha();
		final WritableRaster raster = source.getRaster();
		
		for (int y = 0, i = 0; y < this.getRowCount(); ++y) {
			for (int x = 0; x < this.getColumnCount(); ++x, ++i) {
//				this.data[i] = feature.getValue(source.getRGB(x, y), hasAlpha);
				this.data[i] = feature.getValue(rgb(raster.getDataElements(x, y, null)), hasAlpha);
			}
		}
	}
	
	public static final String toString(final Object array) {
		if (array.getClass().isArray()) {
			final StringBuilder resultBuilder = new StringBuilder();
			final int n = Array.getLength(array);
			
			resultBuilder.append('[');
			
			if (0 < n) {
				resultBuilder.append(Array.get(array, 0));
				
				for (int i = 1; i < n; ++i) {
					resultBuilder.append(' ').append(Array.get(array, i));
				}
			}
			
			resultBuilder.append(']');
			
			return resultBuilder.toString();
		} else {
			return array.toString();
		}
	}
	
	@Override
	public final int getValue(final int index) {
		return this.data[index];
	}
	
	@Override
	public final int setValue(final int index, final int value) {
		final int oldValue = this.data[index];
		
		this.data[index] = (char) value;
		
		return oldValue;
	}
	
	@Override
	public final float getFloatValue(final int index) {
		return this.getValue(index);
	}
	
	@Override
	public final float setFloatValue(final int index, final float value) {
		return this.setValue(index, (int) value);
	}
	
	@Override
	public final String toString() {
		return "Image(rowCount(" + this.getRowCount() + "), " + "columnCount(" + this.getColumnCount() + "))";
	}
	
	public static final int rgb(final Object rgb) {
		int result = 0;
		final int n = Array.getLength(rgb);
		
		for (int i = n - 1; 0 <= i; --i) {
			result = (result << 8) | (0x000000FF & ((Number) Array.get(rgb, i)).intValue());
		}
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2013-01-25)
	 */
	public static enum Feature {
		
		RED {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				return (char) color(rgb, hasAlpha).getRed();
			}
			
			@Override
			public final int getResultChannelCount() {
				return 1;
			}
			
		}, GREEN {
				
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				return (char) color(rgb, hasAlpha).getGreen();
			}
			
			@Override
			public final int getResultChannelCount() {
				return 1;
			}
			
		}, BLUE {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				return (char) color(rgb, hasAlpha).getBlue();
			}
			
			@Override
			public final int getResultChannelCount() {
				return 1;
			}
			
		}, ALPHA {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				return (char) color(rgb, hasAlpha).getAlpha();
			}
			
			@Override
			public final int getResultChannelCount() {
				return 1;
			}
			
		}, MIN_RGB {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				final Color color = color(rgb, hasAlpha);
				
				return (char) min(min(color.getRed(), color.getGreen()), color.getBlue());
			}
			
			@Override
			public final int getResultChannelCount() {
				return 1;
			}
			
		}, MIN_RGBA {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				final Color color = color(rgb, hasAlpha);
				
				return (char) min(min(color.getRed(), color.getGreen()), min(color.getBlue(), color.getAlpha()));
			}
			
			@Override
			public final int getResultChannelCount() {
				return 1;
			}
			
		}, MAX_RGB {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				final Color color = color(rgb, hasAlpha);
				
				return (char) max(max(color.getRed(), color.getGreen()), color.getBlue());
			}
			
			@Override
			public final int getResultChannelCount() {
				return 1;
			}
			
		}, MAX_RGBA {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				final Color color = color(rgb, hasAlpha);
				
				return (char) max(max(color.getRed(), color.getGreen()), max(color.getBlue(), color.getAlpha()));
			}
			
			@Override
			public final int getResultChannelCount() {
				return 1;
			}
			
		}, HUE {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				return (char) (hsb(rgb, hasAlpha)[0] * 255);
			}
			
			@Override
			public final int getResultChannelCount() {
				return 1;
			}
			
		}, SATURATION {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				return (char) (hsb(rgb, hasAlpha)[1] * 255);
			}
			
			@Override
			public final int getResultChannelCount() {
				return 1;
			}
			
		}, BRIGHTNESS {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				return (char) (hsb(rgb, hasAlpha)[2] * 255);
			}
			
			@Override
			public final int getResultChannelCount() {
				return 1;
			}
			
		}, TO_UINT_8 {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				return (char) (rgb & 0xFF);
			}
			
			@Override
			public final int getResultChannelCount() {
				return 1;
			}
			
		};
		
		public abstract int getResultChannelCount();
		
		public abstract char getValue(int rgb, boolean hasAlpha);
		
		public static final float[] hsb(final int rgb, final boolean hasAlpha) {
			final Color color = color(rgb, hasAlpha);
			final float[] hsb = new float[3];
			Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
			
			return hsb;
		}
		
		public static final Color color(final int rgb, final boolean hasAlpha) {
			return hasAlpha ? new Color(rgb, true) : new Color(rgb);
		}
		
	}
	
}
