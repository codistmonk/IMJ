package imj3.draft.machinelearning;

import imj3.draft.machinelearning.DataSource.Metadata;
import imj3.draft.machinelearning.NearestNeighborClassifier.Prototype;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-04)
 * @param <C>
 */
public final class BufferedDataSource<M extends Metadata, C extends ClassifierClass> implements DataSource<M, C> {
	
	private final DataSource<M, C> source;
	
	private final List<Classification<C>> dataset;
	
	private final int bufferLimit;
	
	public BufferedDataSource(final DataSource<M, C> source) {
		this(source, 0);
	}
	
	public BufferedDataSource(final DataSource<M, C> source, final int bufferLimit) {
		this.source = source;
		
		if (bufferLimit <= 0) {
			this.dataset = new ArrayList<>();
			
			Tools.debugPrint("Buffering...");
			
			for (final Classification<C> classification : source) {
				copyTo(this.dataset, classification);
			}
			
			this.bufferLimit = this.dataset.size();
			
			Tools.debugPrint("Buffering", this.bufferLimit, "elements done");
		} else {
			this.dataset = null;
			this.bufferLimit = bufferLimit;
		}
	}
	
	public final DataSource<M, C> getSource() {
		return this.source;
	}
	
	public final int getBufferLimit() {
		return this.bufferLimit;
	}
	
	public final List<Classification<C>> getDataset() {
		return this.dataset;
	}
	
	@Override
	public final M getMetadata() {
		return this.getSource().getMetadata();
	}
	
	@Override
	public final Iterator<Classification<C>> iterator() {
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
	public final class BufferedIterator implements Iterator<Classification<C>> {
		
		private final int bufferLimit = BufferedDataSource.this.getBufferLimit();
		
		private final Iterator<Classification<C>> i = BufferedDataSource.this.getSource().iterator();
		
		private final List<Classification<C>> buffer = new ArrayList<>(this.bufferLimit);
		
		private Iterator<Classification<C>> j = this.buffer.iterator();
		
		public final List<Classification<C>> getBuffer() {
			return this.buffer;
		}
		
		@Override
		public final boolean hasNext() {
			return this.j.hasNext() || this.i.hasNext();
		}
		
		@Override
		public final Classification<C> next() {
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
	
	@SuppressWarnings("unchecked")
	public static final <C extends ClassifierClass> void copyTo(final Collection<Classification<C>> buffer, final Classification<C> classification) {
		final Prototype prototype = Tools.cast(Prototype.class, classification.getClassifierClass());
		
		if (prototype != null && classification.getInput() == prototype.toArray()) {
			final double[] input = classification.getInput().clone();
			
			buffer.add(new Classification<>(input,
					(C) new Prototype(input), classification.getScore()));
		} else {
			buffer.add(new Classification<>(classification.getInput().clone(),
					classification.getClassifierClass(), classification.getScore()));
		}
	}
	
}
