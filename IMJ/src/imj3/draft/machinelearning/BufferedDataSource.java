package imj3.draft.machinelearning;

import imj3.draft.machinelearning.NearestNeighborClassifier.Prototype;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-04)
 * @param <C>
 */
public final class BufferedDataSource<C extends ClassifierClass> implements DataSource<C> {
	
	private final int dimension;
	
	private final List<Classification<C>> dataset;
	
	@SuppressWarnings("unchecked")
	public BufferedDataSource(final DataSource<C> source) {
		this.dimension = source.getDimension();
		this.dataset = new ArrayList<>();
		
		Tools.debugPrint("Buffering...");
		
		for (final Classification<C> classification : source) {
			final Prototype prototype = Tools.cast(Prototype.class, classification.getClassifierClass());
			
			if (prototype != null && classification.getInput() == prototype.getDatum()) {
				final double[] input = classification.getInput().clone();
				
				this.dataset.add(new Classification<>(input,
						(C) new Prototype(input), classification.getScore()));
			} else {
				this.dataset.add(new Classification<>(classification.getInput().clone(),
						classification.getClassifierClass(), classification.getScore()));
			}
		}
		
		Tools.debugPrint("Buffering", this.dataset.size(), "elements done");
	}
	
	public final List<Classification<C>> getDataset() {
		return this.dataset;
	}
	
	@Override
	public final Iterator<Classification<C>> iterator() {
		return this.getDataset().iterator();
	}
	
	@Override
	public final int getDimension() {
		return this.dimension;
	}
	
	private static final long serialVersionUID = -3379089397400242050L;
	
}
