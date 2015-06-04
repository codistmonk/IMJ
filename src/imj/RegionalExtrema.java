package imj;

import multij.primitivelists.IntList;

/**
 * @author codistmonk (creation 2013-01-26)
 */
public abstract class RegionalExtrema extends Labeling {
	
	private final IntList component;
	
	private final IntList componentBorder;
	
	private final IntList componentOutside;
	
	private int labelCount;
	
	public RegionalExtrema(final Image image, final int[] connectivity) {
		this(image, null, connectivity);
	}
	
	public RegionalExtrema(final Image image, final Image result, final int[] connectivity) {
		super(image, result);
		this.component = new IntList();
		this.componentBorder = new IntList();
		this.componentOutside = new IntList();
		
		final int pixelCount = this.getPixelCount();
		final Neighborhood neighborhood = this.new Neighborhood(connectivity);
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			this.getResult().setValue(pixel, STATUS_UNKNOWN);
		}
		
		this.componentOutside.add(0);
		
		while (!this.componentOutside.isEmpty()) {
			final int componentPixel0 = this.componentOutside.remove(0);
			final int componentValue = image.getValue(componentPixel0);
			this.componentBorder.add(componentPixel0);
			int componentStatus = STATUS_MINIMUM;
			
			while (!this.componentBorder.isEmpty()) {
				final int componentPixel = this.componentBorder.remove(0);
				
				if (this.getResult().getValue(componentPixel) <= STATUS_PENDING) {
					this.getResult().setValue(componentPixel, STATUS_PENDING);
					this.component.add(componentPixel);
					
					neighborhood.reset(componentPixel);
					
					while (neighborhood.hasNext()) {
						final int neighbor = neighborhood.getNext();
						final int neighborValue = image.getValue(neighbor);
						final int neighborStatus = this.getResult().getValue(neighbor);
						final int comparison = this.compare(neighborValue, componentValue);
						
						if (comparison < 0) {
							componentStatus = STATUS_NONMINIMUM;
							
							if (neighborStatus == STATUS_UNKNOWN) {
								this.componentOutside.add(neighbor);
								this.getResult().setValue(neighbor, STATUS_SCHEDULED_OUTSIDE);
							}
						} else if (0 < comparison) {
							if (neighborStatus == STATUS_UNKNOWN) {
								this.componentOutside.add(neighbor);
								this.getResult().setValue(neighbor, STATUS_SCHEDULED_OUTSIDE);
							}
						} else {
							if (neighborStatus <= STATUS_SCHEDULED_OUTSIDE) {
								this.componentBorder.add(neighbor);
								this.getResult().setValue(neighbor, STATUS_SCHEDULED_BORDER);
							}
						}
					}
				}
			}
			
			if (!this.component.isEmpty()) {
				final int componentLabel = componentStatus == STATUS_MINIMUM ? ++this.labelCount : componentStatus;
				
				while (!this.component.isEmpty()) {
					final int componentPixel = this.component.remove(0);
					
					assert this.getResult().getValue(componentPixel) == STATUS_PENDING;
					
					this.getResult().setValue(componentPixel, componentLabel);
				}
			}
		}
	}
	
	/**
	 * Called during initialization.
	 * 
	 * @param value1
	 * <br>Range: any int
	 * @param value2
	 * <br>Range: any int
	 * @return
	 * <br>Range: any int
	 */
	protected abstract int compare(int value1, int value2);
	
	/**
	 * {@value}.
	 */
	private static final int STATUS_UNKNOWN = -4;
	
	/**
	 * {@value}.
	 */
	private static final int STATUS_SCHEDULED_OUTSIDE = -3;
	
	/**
	 * {@value}.
	 */
	private static final int STATUS_SCHEDULED_BORDER = -2;
	
	/**
	 * {@value}.
	 */
	private static final int STATUS_PENDING = -1;
	
	/**
	 * {@value}.
	 */
	private static final int STATUS_NONMINIMUM = 0;
	
	/**
	 * {@value}.
	 */
	private static final int STATUS_MINIMUM = 1;
	
}
