package imj;

/**
 * @author codistmonk (creation 2013-01-28)
 */
public abstract class Reconstruction extends Labeling {
	
	private final Neighborhood neighborhood;
	
	private final IntList todo;
	
	protected Reconstruction(final Image mask, final Image marker, final int[] connectivity) {
		super(mask, marker);
		this.neighborhood = this.new Neighborhood(connectivity);
		this.todo = new IntList();
		
		this.pass1RasterOrder();
		this.pass2AntirasterOrder();
		this.pass3Propagation();
	}
	
	protected abstract int getExtremalMarkerValue(int current, int candidate);
	
	protected abstract int mergeMaskAndMarkerValues(int maskValue, int markerValue);
	
	protected abstract boolean acceptNeighborMarkerValue(int neighborMarkerValue, int threshold);
	
	private final void pass1RasterOrder() {
		final Image mask = this.getImage();
		final Image marker = this.getResult();
		final int pixelCount = this.getPixelCount();
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			int extremalNeighborMarkerValue = marker.getValue(pixel);
			
			this.neighborhood.reset(pixel);
			
			while (this.neighborhood.hasNext()) {
				final int neighbor = this.neighborhood.getNext();
				
				if (neighbor < pixel) {
					extremalNeighborMarkerValue = this.getExtremalMarkerValue(extremalNeighborMarkerValue, marker.getValue(neighbor));
				}
			}
			
			marker.setValue(pixel, this.mergeMaskAndMarkerValues(mask.getValue(pixel), extremalNeighborMarkerValue));
		}
	}
	
	private final void pass2AntirasterOrder() {
		final Image mask = this.getImage();
		final Image marker = this.getResult();
		final int pixelCount = this.getPixelCount();
		
		for (int pixel = pixelCount - 1; 0 <= pixel; --pixel) {
			final int pixelMarkerValue = marker.getValue(pixel);
			int extremalNeighborMarkerValue = pixelMarkerValue;
			
			this.neighborhood.reset(pixel);
			
			while (this.neighborhood.hasNext()) {
				final int neighbor = this.neighborhood.getNext();
				
				if (pixel < neighbor) {
					final int neighborMarkerValue = marker.getValue(neighbor);
					extremalNeighborMarkerValue = this.getExtremalMarkerValue(extremalNeighborMarkerValue, neighborMarkerValue);
					
					if (this.acceptNeighborMarkerValue(neighborMarkerValue, pixelMarkerValue) &&
							this.acceptNeighborMarkerValue(neighborMarkerValue, mask.getValue(neighbor))) {
						this.todo.add(pixel);
					}
				}
			}
			
			marker.setValue(pixel, this.mergeMaskAndMarkerValues(mask.getValue(pixel), extremalNeighborMarkerValue));
		}
	}
	
	private final void pass3Propagation() {
		final Image mask = this.getImage();
		final Image marker = this.getResult();
		
		while (!this.todo.isEmpty()) {
			final int pixel = this.todo.remove(0);
			final int pixelMarkerValue = marker.getValue(pixel);
			
			this.neighborhood.reset(pixel);
			
			while (this.neighborhood.hasNext()) {
				final int neighbor = this.neighborhood.getNext();
				final int neighborMarkerValue = marker.getValue(neighbor);
				
				if (this.acceptNeighborMarkerValue(neighborMarkerValue, pixelMarkerValue)) {
					final int neighborMaskValue = mask.getValue(neighbor);
					
					if (neighborMaskValue != neighborMarkerValue) {
						marker.setValue(neighbor, this.mergeMaskAndMarkerValues(neighborMaskValue, pixelMarkerValue));
						this.todo.add(neighbor);
					}
				}
			}
		}
	}
	
}
