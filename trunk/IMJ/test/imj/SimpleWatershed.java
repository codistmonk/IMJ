package imj;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * @author codistmonk (creation 2013-01-24)
 */
public final class SimpleWatershed extends Labeling {
	
	private final int imageMinimum;
	
	private final int imageValueCount;
	
	private final IntList[] todos;
	
	public SimpleWatershed(final Image image) {
		super(image);
		
		{
			int minimum = Integer.MAX_VALUE;
			int maximum = Integer.MIN_VALUE;
			
			for (int i = 0; i < this.getPixelCount(); ++i) {
				final int value = image.getValue(i);
				minimum = min(minimum, value);
				maximum = max(maximum, value);
			}
			
			this.imageMinimum = minimum;
			this.imageValueCount = 1 + maximum - minimum;
		}
		
		this.todos = new IntList[this.imageValueCount];
		
		this.run();
	}
	
	private final void run() {
		int labelCount = 0;
		
		for (int pixel = 0; pixel < this.getPixelCount(); ++pixel) {
			final int value = this.getImage().getValue(pixel);
			
			if (value == this.imageMinimum && this.getLabels().getValue(pixel) == 0) {
				this.markComponent(pixel, ++labelCount);
			}
		}
		
		for (int level = 1; level < this.imageValueCount; ++level) {
			final IntList todo = this.todos[level];
			
			if (todo != null) {
				while (!todo.isEmpty()) {
					this.markPixelFromNeighborAndScheduleUnmarkedNeighbors(todo.remove(0), todo);
				}
			}
			
			for (int pixel = 0; pixel < this.getPixelCount(); ++pixel) {
				final int value = this.getImage().getValue(pixel);
				
				if (value == this.imageMinimum + level && this.getLabels().getValue(pixel) == 0) {
					this.markComponent(pixel, ++labelCount);
				}
			}
		}
	}
	
	private final void markPixelFromNeighborAndScheduleUnmarkedNeighbors(final int pixel, final IntList todo) {
		if (this.getLabels().getValue(pixel) == 0) {
			final int value = this.getImage().getValue(pixel);
			this.getLabels().setValue(pixel, this.getLabelFromNeighbor(pixel, value));
			this.scheduleNeighbors(pixel, value, todo);
		}
	}
	
	private final int getLabelFromNeighbor(final int pixel, final int value) {
		final int rowIndex = this.getRowIndex(pixel);
		final int columnIndex = this.getColumnIndex(pixel);
		
		if (this.hasNorth(rowIndex)) {
			final int neighbor = this.north(pixel);
			final int label = this.getLabels().getValue(neighbor);
			
			if (0 < label) {
				return label;
			}
		}
		
		if (this.hasWest(columnIndex)) {
			final int neighbor = this.west(pixel);
			final int label = this.getLabels().getValue(neighbor);
			
			if (0 < label) {
				return label;
			}
		}
		
		if (this.hasEast(columnIndex)) {
			final int neighbor = this.east(pixel);
			final int label = this.getLabels().getValue(neighbor);
			
			if (0 < label) {
				return label;
			}
		}
		
		if (this.hasSouth(rowIndex)) {
			final int neighbor = this.south(pixel);
			final int label = this.getLabels().getValue(neighbor);
			
			if (0 < label) {
				return label;
			}
		}
		
		throw new IllegalStateException();
	}
	
	public final void markComponent(final int pixel, final int label) {
		final int value = this.getImage().getValue(pixel);
		final IntList todo = new IntList();
		todo.add(pixel);
		
		while (!todo.isEmpty()) {
			final int p = todo.remove(0);
			
			if (this.getLabels().getValue(p) == 0) {
				this.scheduleNeighbors(p, value, todo);
			}
			
			this.getLabels().setValue(p, label);
		}
	}
	
	private final void scheduleNeighbors(final int pixel, final int value, final IntList todo) {
		final int rowIndex = this.getRowIndex(pixel);
		final int columnIndex = this.getColumnIndex(pixel);
		
		if (this.hasNorth(rowIndex)) {
			scheduleNeighbor(value, todo, this.north(pixel));
		}
		
		if (this.hasWest(columnIndex)) {
			scheduleNeighbor(value, todo, this.west(pixel));
		}
		
		if (this.hasEast(columnIndex)) {
			scheduleNeighbor(value, todo, this.east(pixel));
		}
		
		if (this.hasSouth(rowIndex)) {
			this.scheduleNeighbor(value, todo, this.south(pixel));
		}
	}
	
	private final void scheduleNeighbor(final int value, final IntList todo, final int neighbor) {
		final int neighborValue = this.getImage().getValue(neighbor);
		
		if (neighborValue == value) {
			if (this.getLabels().getValue(neighbor) == 0) {
				todo.add(neighbor);
			}
		} else if (value < neighborValue) {
			this.schedule(neighbor, neighborValue);
		}
	}
	
	private final void schedule(final int pixel, final int value) {
		final int i = value - this.imageMinimum;
		IntList todo = this.todos[i];
		
		if (todo == null) {
			todo = new IntList();
			this.todos[i] = todo;
		}
		
		todo.add(pixel);
	}
	
}
