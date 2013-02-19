package imj.apps.modules;

import static imj.MorphologicalOperations.StructuringElement.newDisk;
import static imj.MorphologicalOperations.StructuringElement.newRing;
import static java.lang.Double.parseDouble;
import static net.sourceforge.aprog.tools.Tools.cast;
import imj.IMJTools;
import imj.Image;
import imj.Labeling.NeighborhoodShape.Distance;
import imj.apps.modules.FilteredImage.Filter;
import imj.apps.modules.FilteredImage.StructuringElementFilter;

import java.util.Locale;

import net.sourceforge.aprog.context.Context;
import net.sourceforge.aprog.events.Variable;
import net.sourceforge.aprog.events.Variable.Listener;
import net.sourceforge.aprog.events.Variable.ValueChangedEvent;

/**
 * @author codistmonk (creation 2013-02-18)
 */
public abstract class ViewFilter extends Plugin implements Filter {
	
	protected ViewFilter(final Context context) {
		super(context);
	}
	
	/**
	 * @author codistmonk (creation 2013-02-19)
	 */
	public static abstract class FromFilter extends ViewFilter {
		
		private Filter filter;
		
		protected FromFilter(final Context context) {
			super(context);
			
			this.getParameters().put("structuringElement", "disk 1 chessboard");
			
			final Variable<Image> imageVariable = context.getVariable("image");
			
			imageVariable.addListener(new Listener<Image>() {
				
				@Override
				public final void valueChanged(final ValueChangedEvent<Image, ?> event) {
					FromFilter.this.setFilter(FromFilter.this.getFilter());
				}
				
			});
		}
		
		public final Filter getFilter() {
			return this.filter;
		}
		
		public final void setFilter(final Filter filter) {
			this.filter = filter;
			
			if (filter instanceof StructuringElementFilter) {
				final Image image = this.getContext().get("image");
				final FilteredImage filteredImage = cast(FilteredImage.class, image);
				
				((StructuringElementFilter) filter).setImage(filteredImage != null ? filteredImage.getSource() : image);
			}
		}
		
		@Override
		public final int getNewValue(final int index, final int oldValue) {
			return this.getFilter().getNewValue(index, oldValue);
		}
		
		public final int[] parseStructuringElement() {
			final String[] structuringElementParameters = this.getParameters().get("structuringElement").trim().split("\\s+");
			final String shape = structuringElementParameters[0];
			
			if ("ring".equals(shape)) {
				final double innerRadius = parseDouble(structuringElementParameters[1]);
				final double outerRadius = parseDouble(structuringElementParameters[2]);
				final Distance distance = Distance.valueOf(structuringElementParameters[3].toUpperCase(Locale.ENGLISH));
				
				return newRing(innerRadius, outerRadius, distance);
			}
			
			if ("disk".equals(shape)) {
				final double radius = parseDouble(structuringElementParameters[1]);
				final Distance distance = Distance.valueOf(structuringElementParameters[2].toUpperCase(Locale.ENGLISH));
				
				return newDisk(radius, distance);
			}
			
			throw new IllegalArgumentException("Invalid structuring element shape: " + shape);
		}
		
	}
	
}
