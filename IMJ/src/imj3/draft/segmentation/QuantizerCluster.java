package imj3.draft.segmentation;

import java.util.Locale;

/**
 * @author codistmonk (creation 2015-01-16)
 */
public final class QuantizerCluster extends QuantizerNode {
	
	private String name = DEFAULT_NAME;
	
	private int label = DEFAULT_LABEL;
	
	private int minimumSegmentSize = DEFAULT_MINIMUM_SEGMENT_SIZE;
	
	private int maximumSegmentSize = DEFAULT_MAXIMUM_SEGMENT_SIZE;
	
	private int maximumPrototypeCount = DEFAULT_MAXIMUM_PROTOTYPE_COUNT;
	
	@Override
	public final QuantizerCluster copy() {
		final QuantizerCluster result = new QuantizerCluster();
		
		result.name = this.name;
		result.label = this.label;
		result.minimumSegmentSize = this.minimumSegmentSize;
		result.maximumSegmentSize = this.maximumSegmentSize;
		result.maximumPrototypeCount = this.maximumPrototypeCount;
		
		return this.copyChildrenTo(result).setUserObject();
	}
	
	@Override
	public final QuantizerCluster setUserObject() {
		this.setUserObject(this.new UserObject() {
			
			@Override
			public final String toString() {
				final boolean showMaximum = QuantizerCluster.this.getMaximumSegmentSize() != Integer.MAX_VALUE;
				
				return QuantizerCluster.this.getName() + " ("
						+ QuantizerCluster.this.getLabelAsString() + " "
						+ QuantizerCluster.this.getMinimumSegmentSizeAsString() + ".."
						+ (showMaximum ? QuantizerCluster.this.getMaximumSegmentSizeAsString() : "")
						+ ")";
			}
			
			private static final long serialVersionUID = 1507012060737286549L;
			
		});
		
		return this;
	}
	
	public final String getName() {
		return this.name;
	}
	
	public final QuantizerCluster setName(final String name) {
		this.name = name;
		
		return this;
	}
	
	@Override
	public final Quantizer getParent() {
		return (Quantizer) super.getParent();
	}
	
	public final Quantizer getQuantizer() {
		return this.getParent();
	}
	
	public final int getLabel() {
		return this.label;
	}
	
	public final QuantizerCluster setLabel(final int label) {
		this.label = label;
		
		return this;
	}
	
	public final String getLabelAsString() {
		return "#" + Integer.toHexString(this.label).toUpperCase(Locale.ENGLISH);
	}
	
	public final QuantizerCluster setLabel(final String labelAsString) {
		this.setLabel(parseARGB(labelAsString));
		
		return this;
	}
	
	public final int getMinimumSegmentSize() {
		return this.minimumSegmentSize;
	}
	
	public final QuantizerCluster setMinimumSegmentSize(final int minimumSegmentSize) {
		this.minimumSegmentSize = minimumSegmentSize;
		
		return this;
	}
	
	public final String getMinimumSegmentSizeAsString() {
		return Integer.toString(this.getMinimumSegmentSize());
	}
	
	public final QuantizerCluster setMinimumSegmentSize(final String minimumSegmentSizeAsString) {
		this.setMinimumSegmentSize(Integer.parseInt(minimumSegmentSizeAsString));
		
		return this;
	}
	
	public final int getMaximumSegmentSize() {
		return this.maximumSegmentSize;
	}
	
	public final QuantizerCluster setMaximumSegmentSize(final int maximumSegmentSize) {
		this.maximumSegmentSize = maximumSegmentSize;
		
		return this;
	}
	
	public final String getMaximumSegmentSizeAsString() {
		return Integer.toString(this.getMaximumSegmentSize());
	}
	
	public final QuantizerCluster setMaximumSegmentSize(final String maximumSegmentSizeAsString) {
		this.setMaximumSegmentSize(Integer.parseInt(maximumSegmentSizeAsString));
		
		return this;
	}
	
	public final int getMaximumPrototypeCount() {
		return this.maximumPrototypeCount;
	}
	
	public final QuantizerCluster setMaximumPrototypeCount(final int maximumPrototypeCount) {
		if (maximumPrototypeCount <= 0) {
			throw new IllegalArgumentException();
		}
		
		this.maximumPrototypeCount = maximumPrototypeCount;
		
		return this;
	}
	
	public final String getMaximumPrototypeCountAsString() {
		return Integer.toString(this.getMaximumPrototypeCount());
	}
	
	public final QuantizerCluster setMaximumPrototypeCount(final String maximumPrototypeCountAsString) {
		this.setMaximumPrototypeCount(Integer.parseInt(maximumPrototypeCountAsString));
		
		return this;
	}
	
	public final double distanceTo(final int[] values, final double maximum) {
		final int n = this.getChildCount();
		double result = Double.POSITIVE_INFINITY;
		
		for (int i = 0; i < n; ++i) {
			final double distance = ((QuantizerPrototype) this.getChildAt(i)).distanceTo(values, maximum);
			
			if (distance < result) {
				result = distance;
			}
		}
		
		return result;
	}
	
	@Override
	public final <V> V accept(final Visitor<V> visitor) {
		return visitor.visit(this);
	}
	
	private static final long serialVersionUID = -3727849715989585298L;
	
	public static final String DEFAULT_NAME = "cluster";
	
	public static final int DEFAULT_LABEL = 1;
	
	public static final int DEFAULT_MINIMUM_SEGMENT_SIZE = 0;
	
	public static final int DEFAULT_MAXIMUM_SEGMENT_SIZE = Integer.MAX_VALUE;
	
	public static final int DEFAULT_MAXIMUM_PROTOTYPE_COUNT = 1;
	
}