package imj3.machinelearning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import multij.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public final class BufferedDataSource extends DataSource.Abstract<DataSource> {
	
	private final List<Datum> dataset;
	
	private final int bufferLimit;
	
	public BufferedDataSource(final DataSource source, final int bufferLimit) {
		super(source);
		
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
	
	public final int getBufferLimit() {
		return this.bufferLimit;
	}
	
	public final List<Datum> getDataset() {
		return this.dataset;
	}
	
	@Override
	public final Iterator iterator() {
		return this.getDataset() != null ? Iterator.wrap(this.getDataset().iterator()) : this.new BufferedIterator();
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
	public final class BufferedIterator extends Iterator.Abstract<Iterator> {
		
		private final int bufferLimit = BufferedDataSource.this.getBufferLimit();
		
		private final Iterator i = BufferedDataSource.this.getSource().iterator();
		
		private final List<Datum> buffer = new ArrayList<>(this.bufferLimit);
		
		private java.util.Iterator<Datum> j = this.buffer.iterator();
		
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
		
		private static final long serialVersionUID = -3044893015706545294L;
		
	}
	
	private static final long serialVersionUID = -3379089397400242050L;
	
	public static final void copyTo(final Collection<Datum> buffer, final Datum classification) {
		buffer.add(classification.copy());
	}
	
	public static final BufferedDataSource buffer(final DataSource source) {
		return buffer(source, 0);
	}
	
	public static final BufferedDataSource buffer(final DataSource source, final int bufferLimit) {
		return new BufferedDataSource(source, bufferLimit);
	}
	
}
