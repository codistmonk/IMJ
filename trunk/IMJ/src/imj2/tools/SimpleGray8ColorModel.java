package imj2.tools;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.Serializable;

/**
 * @author codistmonk (creation 2014-06-15)
 */
public final class SimpleGray8ColorModel extends ColorModel implements Serializable {
	
	public SimpleGray8ColorModel() {
		super(8);
	}
	
	public final int getGray(final int pixel) {
		return pixel & 0xFF;
	}
	
	@Override
	public final int getRed(final int pixel) {
		return this.getGray(pixel);
	}
	
	@Override
	public final int getGreen(final int pixel) {
		return this.getGray(pixel);
	}
	
	@Override
	public final int getBlue(final int pixel) {
		return this.getGray(pixel);
	}
	
	@Override
	public final int getAlpha(final int pixel) {
		return 0xFF;
	}
	
	@Override
	public final WritableRaster createCompatibleWritableRaster(final int w, final int h) {
		return Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, w, h, 1, null);
	}
	
	@Override
	public final boolean isCompatibleRaster(final Raster raster) {
		return true;
	}
	
	@Override
	public final Object getDataElements(final int rgb, final Object pixel) {
		return new byte[] { (byte) rgb };
	}
	
	public static BufferedImage newByteGrayAWTImage(final int width, final int height) {
		final ColorModel colorModel = new SimpleGray8ColorModel();
		
		return new BufferedImage(colorModel
				, colorModel.createCompatibleWritableRaster(width, height), false, null);
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -7587662751437452247L;
	
}
