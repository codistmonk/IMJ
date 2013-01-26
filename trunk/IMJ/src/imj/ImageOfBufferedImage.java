package imj;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.aprog.tools.Tools.debugPrint;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.lang.reflect.Array;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2013-01-23)
 */
public final class ImageOfBufferedImage extends Image.Abstract {
	
	private final char[] data;
	
	public ImageOfBufferedImage(final BufferedImage source, final Feature feature) {
		super(source.getHeight(), source.getWidth());
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
	public final String toString() {
		return "Image(rowCount(" + this.getRowCount() + "), " + "columnCount(" + this.getColumnCount() + "))";
	}
	
	public static final int rgb(final Object rgb) {
		int result = 0;
		final int n = Array.getLength(rgb);
		
		for (int i = 0; i < n; ++i) {
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
			
		}, GREEN {
				
				@Override
				public final char getValue(final int rgb, final boolean hasAlpha) {
					return (char) color(rgb, hasAlpha).getGreen();
				}
				
		}, BLUE {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				return (char) color(rgb, hasAlpha).getBlue();
			}
			
		}, ALPHA {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				return (char) color(rgb, hasAlpha).getAlpha();
			}
			
		}, MIN_RGB {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				final Color color = color(rgb, hasAlpha);
				
				return (char) min(min(color.getRed(), color.getGreen()), color.getBlue());
			}
			
		}, MIN_RGBA {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				final Color color = color(rgb, hasAlpha);
				
				return (char) min(min(color.getRed(), color.getGreen()), min(color.getBlue(), color.getAlpha()));
			}
			
		}, MAX_RGB {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				final Color color = color(rgb, hasAlpha);
				
				return (char) max(max(color.getRed(), color.getGreen()), color.getBlue());
			}
			
		}, MAX_RGBA {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				final Color color = color(rgb, hasAlpha);
				
				return (char) max(max(color.getRed(), color.getGreen()), max(color.getBlue(), color.getAlpha()));
			}
			
		}, HUE {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				return (char) (hsb(rgb, hasAlpha)[0] * 255);
			}
			
		}, SATURATION {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				return (char) (hsb(rgb, hasAlpha)[1] * 255);
			}
			
		}, BRIGHTNESS {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				return (char) (hsb(rgb, hasAlpha)[2] * 255);
			}
			
		}, TO_UINT_8 {
			
			@Override
			public final char getValue(final int rgb, final boolean hasAlpha) {
				return (char) (rgb & 0xFF);
			}
			
		};
		
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
