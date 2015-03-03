package imj3.draft.machinelearning;

import imj3.draft.machinelearning.DataSource.Metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-04)
 * @param <D>
 */
public final class BufferedDataSource<M extends Metadata> implements DataSource<M> {
	
	private final DataSource<M> source;
	
	private final List<Datum> dataset;
	
	private final int bufferLimit;
	
	public BufferedDataSource(final DataSource<M> source, final int bufferLimit) {
		this.source = source;
		
		if (bufferLimit <= 0) {
			this.dataset = new ArrayList<>();
			
			Tools.debugPrint("Buffering...");
			
			for (final Datum classification : source) {
				copyTo(this.dataset, classification);
			}
			
			this.bufferLimit = this.dataset.size();
			
			Tools.debugPrint("Buffering", this.bufferLimit, "elements done");
		} else {
			this.dataset = null;
			this.bufferLimit = bufferLimit;
		}
	}
	
	public final DataSource<M> getSource() {
		return this.source;
	}
	
	public final int getBufferLimit() {
		return this.bufferLimit;
	}
	
	public final List<Datum> getDataset() {
		return this.dataset;
	}
	
	@Override
	public final M getMetadata() {
		return this.getSource().getMetadata();
	}
	
	@Override
	public final Iterator<Datum> iterator() {
		return this.getDataset() != null ? this.getDataset().iterator() : this.new BufferedIterator();
	}
	
	@Override
	public final int getInputDimension() {
		return this.getSource().getInputDimension();
	}
	
	@Override
	public final int getClassDimension() {
		return this.getSource().getClassDimension();
	}
	
	/**
	 * @author codistmonk (creation 2015-02-08)
	 */
	public final class BufferedIterator implements Iterator<Datum> {
		
		private final int bufferLimit = BufferedDataSource.this.getBufferLimit();
		
		private final Iterator<Datum> i = BufferedDataSource.this.getSource().iterator();
		
		private final List<Datum> buffer = new ArrayList<>(this.bufferLimit);
		
		private Iterator<Datum> j = this.buffer.iterator();
		
		public final List<Datum> getBuffer() {
			return this.buffer;
		}
		
		@Override
		public final boolean hasNext() {
			return this.j.hasNext() || this.i.hasNext();
		}
		
		@Override
		public final Datum next() {
			if (!this.j.hasNext()) {
				this.getBuffer().clear();
				
				for (int k = 0; k < this.bufferLimit && this.i.hasNext() ; ++k) {
					copyTo(this.getBuffer(), this.i.next());
				}
				
				this.j = this.buffer.iterator();
			}
			
			return this.j.next();
		}
		
	}
	
	private static final long serialVersionUID = -3379089397400242050L;
	
	public static final void copyTo(final Collection<Datum> buffer, final Datum classification) {
		final Datum prototype = classification.getPrototype();
		
		if (prototype != null && classification.getValue() == prototype.getValue()) {
			final double[] input = classification.getValue().clone();
			
			buffer.add(new Datum.Default().setValue(input).setScore(classification.getScore()));
		} else {
			buffer.add(new Datum.Default().setValue(classification.getValue().clone()).setPrototype(prototype).setScore(classification.getScore()));
		}
	}
	
	public static final <M extends Metadata> BufferedDataSource<M> buffer(final DataSource<M> source) {
		return buffer(source, 0);
	}
	
	public static final <M extends Metadata> BufferedDataSource<M> buffer(final DataSource<M> source, final int bufferLimit) {
		return new BufferedDataSource<>(source, bufferLimit);
	}
	
}
