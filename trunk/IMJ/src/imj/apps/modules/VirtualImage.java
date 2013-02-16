package imj.apps.modules;

import imj.Image;
import imj.ImageOfBufferedImage.Feature;

/**
 * @author codistmonk (creation 2013-02-03)
 */
public final class VirtualImage extends Image.Abstract {
	
	private final Image rgbs;
	
	private final Feature feature;
	
	public VirtualImage(final Image rgbs, final Feature feature) {
		super(rgbs.getRowCount(), rgbs.getColumnCount(), rgbs.getChannelCount());
		this.rgbs = rgbs;
		this.feature = feature;
	}
	
	@Override
	public final int getValue(final int index) {
		return this.feature.getValue(this.rgbs.getValue(index), false);
	}
	
	@Override
	public final int setValue(final int index, final int value) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public float getFloatValue(final int index) {
		return this.getValue(index);
	}
	
	@Override
	public final float setFloatValue(final int index, final float value) {
		throw new UnsupportedOperationException();
	}
	
}
