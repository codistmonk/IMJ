package imj;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import loci.poi.util.IntList;
	
/**
 * @author codistmonk (creation 2013-01-24)
 */
public final class FlowWatershed extends Labeling {
	
	private final Image f;
	
	private final IntList stream;
	
	private final IntList unexplored;
	
	private int streamLabel;
	
	public FlowWatershed(final Image image) {
		super(image);
		final int rowCount = image.getRowCount();
		final int columnCount = image.getColumnCount();
		this.f = new ImageOfInts(rowCount, columnCount);
		this.stream = new IntList();
		this.unexplored = new IntList();
		final int lastRowIndex = rowCount - 1;
		final int lastColumnIndex = columnCount - 1;
		
		for (int y = 0; y < rowCount; ++y) {
			for (int x = 0; x < columnCount; ++x) {
				final int value = image.getValue(y, x);
				int f = Integer.MAX_VALUE;
				
				if (0 < y) {
					f = min(f, this.getEdgeScore(value, image.getValue(y - 1, x)));
				}
				
				if (0 < x) {
					f = min(f, this.getEdgeScore(value, image.getValue(y, x - 1)));
				}
				
				if (x < lastColumnIndex) {
					f = min(f, this.getEdgeScore(value, image.getValue(y, x + 1)));
				}
				
				if (y < lastRowIndex) {
					f = min(f, this.getEdgeScore(value, image.getValue(y + 1, x)));
				}
				
				this.f.setValue(y, x, f);
			}
		}
		
		this.run();
	}
	
	private final void run() {
		final int pixelCount = this.getImage().getRowCount() * this.getImage().getColumnCount();
		int labelCount = 0;
		
		for (int pixel = 0; pixel < pixelCount; ++pixel) {
			if (this.getResult().getValue(pixel) == 0) {
				this.computeStream(pixel);
				final int label = this.streamLabel == -1 ? ++labelCount : this.streamLabel;
				final int n = this.stream.size();
				
				for (int i = 0; i < n; ++i) {
					this.getResult().setValue(this.stream.get(i), label);
				}
			}
		}
	}
	
	private final void computeStream(final int pixel) {
		this.stream.clear();
		this.stream.add(pixel);
		this.unexplored.clear();
		this.unexplored.add(pixel);
		
		while (!this.unexplored.isEmpty()) {
//			final int pixel2 = this.unexplored.remove(this.unexplored.size() - 1);
			final int pixel2 = this.unexplored.remove(0);
			boolean breadthFirst = true;
			int pixel3 = this.findClosestUnprocessedNeighbor(pixel2);
			
			while (breadthFirst && 0 <= pixel3) {
				final int label3 = this.getResult().getValue(pixel3);
				
				if (label3 != 0) {
					this.streamLabel = label3;
					return;
				}
				
				if (this.f.getValue(pixel3) < this.f.getValue(pixel2)) {
//				if (this.getImage().getValue(pixel3) < this.getImage().getValue(pixel2)) {
					this.unexplored.clear();
					breadthFirst = false;
				}
				
				this.stream.add(pixel3);
				this.unexplored.add(pixel3);
				this.getResult().setValue(pixel3, -1);
			}
		}
		
		this.streamLabel = -1;
	}
	
	private final int findClosestUnprocessedNeighbor(final int pixel) {
		final int rowIndex = this.getRowIndex(pixel);
		final int columnIndex = this.getColumnIndex(pixel);
		final int pixelValue = this.getImage().getValue(pixel);
		final int smallestDistance = this.f.getValue(pixel);
		
		if (this.hasNorth(rowIndex)) {
			final int neighbor = this.evaluateNeighbor(pixelValue, smallestDistance, this.north(pixel));
			
			if (0 <= neighbor) {
				return neighbor;
			}
		}
		
		if (this.hasWest(columnIndex)) {
			final int neighbor = this.evaluateNeighbor(pixelValue, smallestDistance, this.west(pixel));
			
			if (0 <= neighbor) {
				return neighbor;
			}
		}
		
		if (this.hasEast(columnIndex)) {
			final int neighbor = this.evaluateNeighbor(pixelValue, smallestDistance, this.east(pixel));
			
			if (0 <= neighbor) {
				return neighbor;
			}
		}
		
		if (this.hasSouth(rowIndex)) {
			final int neighbor = this.evaluateNeighbor(pixelValue, smallestDistance, this.south(pixel));
			
			if (0 <= neighbor) {
				return neighbor;
			}
		}
		
		return -1;
	}
	
	private int evaluateNeighbor(final int pixelValue, final int smallestDistance, final int neighbor) {
		if (0 <= this.getResult().getValue(neighbor)) {
			final int distance = this.getEdgeScore(pixelValue, this.getImage().getValue(neighbor));
			
			if (distance == smallestDistance) {
				return neighbor;
			}
		}
		
		return -1;
	}
	
	private final int getEdgeScore(final int value1, final int value2) {
//		return min(value1, value2);
		return abs(value1 - value2);
	}
	
}
