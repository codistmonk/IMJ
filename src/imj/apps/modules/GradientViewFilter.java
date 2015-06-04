package imj.apps.modules;

import static imj.IMJTools.gray;
import imj.Image;
import multij.context.Context;

/**
 * @author codistmonk (creation 2013-06-27)
 */
public final class GradientViewFilter extends ViewFilter {
	
	public GradientViewFilter(final Context context) {
		super(context);
		
		this.getParameters().clear();
	}
	
	@Override
	protected final ComplexFilter newComplexFilter() {
		return new ComplexFilter(false, true) {
			
			@Override
			public final int getNewValue(final int index, final int oldValue,  final Channel channel) {
				final Image image = GradientViewFilter.this.getImage().getSource();
				final int imageColumnCount = image.getColumnCount();
				
				return gray(-SeamGridSegmentationSieve.getCost(image, Channel.Primitive.RGB,
						index / imageColumnCount, index % imageColumnCount));
			}
			
		};
	}
	
}
