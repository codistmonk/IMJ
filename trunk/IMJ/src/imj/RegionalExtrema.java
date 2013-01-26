package imj;

/**
 * @author codistmonk (creation 2013-01-26)
 */
public abstract class RegionalExtrema extends Labeling {
	
	private final IntList component;
	
	private final IntList componentBorder;
	
	private final IntList componentOutside;
	
	public RegionalExtrema(final Image image, final int[] connectivity) {
		super(image);
		this.component = new IntList();
		this.componentBorder = new IntList();
		this.componentOutside = new IntList();
		
		final int pixelCount = this.getPixelCount();
		final Neighborhood neighborhood = this.new Neighborhood(connectivity);
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			this.getLabels().setValue(pixel, STATUS_UNKNOWN);
		}
		
//			debugPrint("labels:", "\n" + RegionalExtremaTest.toString(this.getLabels()));
//			debugPrint("component:", this.component);
//			debugPrint("componentBorder:", this.componentBorder);
//			debugPrint("componentOutside:", this.componentOutside);
		
		this.componentOutside.add(0);
//			debugPrint("componentOutside:", this.componentOutside);
		
		while (!this.componentOutside.isEmpty()) {
			final int componentPixel0 = this.componentOutside.remove(0);
//				debugPrint("componentOutside:", this.componentOutside);
			final int componentValue = image.getValue(componentPixel0);
			this.componentBorder.add(componentPixel0);
//				debugPrint("componentBorder:", this.componentBorder);
			int componentStatus = STATUS_MINIMUM;
			
//				debugPrint(this.componentBorder.isEmpty());
			while (!this.componentBorder.isEmpty()) {
				final int componentPixel = this.componentBorder.remove(0);
//					debugPrint(componentPixel);
//					debugPrint("componentBorder:", this.componentBorder);
				
				if (this.getLabels().getValue(componentPixel) <= STATUS_PENDING) {
					this.getLabels().setValue(componentPixel, STATUS_PENDING);
//						debugPrint("labels:", "\n" + RegionalExtremaTest.toString(this.getLabels()));
					this.component.add(componentPixel);
//						debugPrint("component:", this.component);
					
					neighborhood.reset(componentPixel);
					
					while (neighborhood.hasNext()) {
						final int neighbor = neighborhood.getNext();
						final int neighborValue = image.getValue(neighbor);
						final int neighborStatus = this.getLabels().getValue(neighbor);
						final int comparison = this.compare(neighborValue, componentValue);
						
						if (comparison < 0) {
							componentStatus = STATUS_NONMINIMUM;
							
							if (neighborStatus == STATUS_UNKNOWN) {
								this.componentOutside.add(neighbor);
//									debugPrint("componentOutside:", this.componentOutside);
								this.getLabels().setValue(neighbor, STATUS_SCHEDULED_OUTSIDE);
//									debugPrint("labels:", "\n" + RegionalExtremaTest.toString(this.getLabels()));
							}
						} else if (0 < comparison) {
							if (neighborStatus == STATUS_UNKNOWN) {
								this.componentOutside.add(neighbor);
//									debugPrint("componentOutside:", this.componentOutside);
								this.getLabels().setValue(neighbor, STATUS_SCHEDULED_OUTSIDE);
//									debugPrint("labels:", "\n" + RegionalExtremaTest.toString(this.getLabels()));
							}
						} else {
							if (neighborStatus <= STATUS_SCHEDULED_OUTSIDE) {
								this.componentBorder.add(neighbor);
//									debugPrint("componentBoder:", this.componentBorder);
								this.getLabels().setValue(neighbor, STATUS_SCHEDULED_BORDER);
//									debugPrint("labels:", "\n" + RegionalExtremaTest.toString(this.getLabels()));
							}
						}
					}
				}
			}
			
			while (!this.component.isEmpty()) {
				final int componentPixel = this.component.remove(0);
//					debugPrint("component:", this.component);
				
				if (STATUS_SCHEDULED_BORDER < this.getLabels().getValue(componentPixel)) {
//						debugPrint(componentPixel, this.getLabels().getValue(componentPixel));
				}
				
				assert this.getLabels().getValue(componentPixel) == STATUS_PENDING;
				
				this.getLabels().setValue(componentPixel, componentStatus);
//					debugPrint("labels:", "\n" + RegionalExtremaTest.toString(this.getLabels()));
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
