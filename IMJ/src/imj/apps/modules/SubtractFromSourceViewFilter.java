package imj.apps.modules;

import imj.Image;
import net.sourceforge.aprog.context.Context;

/**
 * @author codistmonk (creation 2013-03-08)
 */
public final class SubtractFromSourceViewFilter extends ViewFilter {
	
	private Image sourceSource;
	
	public SubtractFromSourceViewFilter(final Context context) {
		super(context);
	}
	
	@Override
	protected final void doInitialize() {
		this.sourceImageChanged();
	}
	
	@Override
	protected final void sourceImageChanged() {
		final ViewFilter source = this.getSource();
		
		if (source == null) {
			this.sourceSource = null;
			
			return;
		}
		
		final FilteredImage sourceImage = source.getImage();
		
		if (sourceImage == null) {
			this.sourceSource = null;
			
			return;
		}
		
		this.sourceSource = sourceImage.getSource();
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return new ComplexFilter() {
			
			@Override
			public final int getNewValue(final int index, final int oldValue, final Channel channel) {
				final Image sourceSource = SubtractFromSourceViewFilter.this.getSourceSource();
				final int oldChannelValue = channel.getValue(oldValue);
				
				return sourceSource == null ? oldChannelValue : channel.getValue(sourceSource.getValue(index) - oldChannelValue);
			}
			
		};
	}
	
	final Image getSourceSource() {
		return this.sourceSource;
	}
	
}
