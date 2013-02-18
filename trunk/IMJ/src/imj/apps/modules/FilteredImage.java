package imj.apps.modules;

import imj.Image;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public final class FilteredImage extends Image.Abstract {
	
	private final Image source;
	
	private ViewFilter filter;
	
	public FilteredImage(final Image source) {
		super(source.getRowCount(), source.getColumnCount(), source.getChannelCount());
		this.source = source;
	}
	
	public final Image getSource() {
		return this.source;
	}
	
	public final ViewFilter getFilter() {
		return this.filter;
	}
	
	public final void setFilter(final ViewFilter filter) {
		this.filter = filter;
	}
	
	@Override
	public final int getValue(final int index) {
		return this.getFilter() == null ? this.getSource().getValue(index) :
			this.getFilter().getNewValue(index, this.getSource().getValue(index));
	}
	
	@Override
	public final int setValue(final int index, final int value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final float getFloatValue(final int index) {
		return this.getValue(index);
	}
	
	@Override
	public final float setFloatValue(final int index, float value) {
		return this.setValue(index, (int) value);
	}
	
	/**
	 * @author codistmonk (creation 2013-02-18)
	 */
	public static abstract interface Filter {
		
		public abstract int getNewValue(int index, int oldValue);
		
	}
	
}
