package imj3.draft.machinelearning;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-05)
 */
public final class AssignmentProblem {
	
	static final Random random = new Random(0L);
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		final int n = 256;
		final int d = 1;
		final List<EndPoint<double[]>> sources = new ArrayList<>(n);
		final List<EndPoint<double[]>> targets = new ArrayList<>(n);
		
		for (int i = 0; i < n; ++i) {
			sources.add(new EndPoint<>(random.doubles(d).toArray()));
			targets.add(new EndPoint<>(random.doubles(d).toArray()));
		}
		
		final List<Association<double[]>> associations = new ArrayList<>(n * n);
		
		for (int i = 0; i < n; ++i) {
			final EndPoint<double[]> source = sources.get(i);
			
			for (int j = 0; j < n; ++j) {
				final EndPoint<double[]> target = targets.get(j);
				final double cost = Measure.Predefined.L1.compute(source.getObject(), target.getObject());
				
				associations.add(new Association<>(source, target, cost));
			}
		}
		
		Collections.sort(associations);
		
		final Association<double[]>[] bestAssociation = new Association[n];
		final double[] bestCost = { Double.POSITIVE_INFINITY };
		
		findBestAssignment(associations, bestAssociation, bestCost, new Association[n], new double[1], 0,
				new TicToc(), 4_000L);
		
		Tools.debugPrint(bestCost[0]);
	}
	
	public static final <T> boolean findBestAssignment(final List<Association<T>> associations,
			final Association<T>[] bestAssociation, final double[] bestCost,
			final Association<T>[] currentAssociation, final double[] currentCost,
			final int i, final TicToc timer, final long limit) {
		if (limit <= timer.toc()) {
			return false;
		}
		
		final int n = bestAssociation.length;
		
		for (final Association<T> association : associations) {
			if (association.isLocked()) {
				continue;
			}
			
			final double oldCost = currentCost[0];
			final double newCost = oldCost + association.getCost();
			
			if (bestCost[0] <= newCost) {
				break;
			}
			
			currentAssociation[i] = association;
			currentCost[0] = newCost;
			final int nextI = i + 1;
			
			if (nextI < n) {
				association.getEndPoint1().setLocked(true);
				association.getEndPoint2().setLocked(true);
				final boolean b = findBestAssignment(associations, bestAssociation, bestCost,
						currentAssociation, currentCost, nextI, timer, limit);
				association.getEndPoint1().setLocked(false);
				association.getEndPoint2().setLocked(false);
				
				if (!b) {
					return false;
				}
			} else {
				System.arraycopy(currentAssociation, 0, bestAssociation, 0, n);
				bestCost[0] = newCost;
			}
			
			currentAssociation[i] = null;
			currentCost[0] = oldCost;
		}
		
		return true;
	}
	
	/**
	 * @author codistmonk (creation 2015-02-05)
	 */
	public static final class Association<T> implements Serializable, Comparable<Association<T>> {
		
		private final EndPoint<T> endPoint1, endPoint2;
		
		private final double cost;
		
		public Association(final EndPoint<T> endPoint1, final EndPoint<T> endPoint2, final double cost) {
			this.endPoint1 = endPoint1;
			this.endPoint2 = endPoint2;
			this.cost = cost;
		}
		
		public final EndPoint<T> getEndPoint1() {
			return this.endPoint1;
		}
		
		public final EndPoint<T> getEndPoint2() {
			return this.endPoint2;
		}
		
		public final double getCost() {
			return this.cost;
		}
		
		public final boolean isLocked() {
			return this.getEndPoint1().isLocked() || this.getEndPoint2().isLocked();
		}
		
		private static final long serialVersionUID = 7836354402558855202L;
		
		@Override
		public final int compareTo(final Association<T> that) {
			return Double.compare(this.getCost(), that.getCost());
		}
		
	}
	
	/**
	 * @author codistmonk (creation 2015-02-05)
	 */
	public static final class EndPoint<T> implements Serializable {
		
		private final T object;
		
		private boolean locked;
		
		public EndPoint(final T object) {
			this.object = object;
		}
		
		public final boolean isLocked() {
			return this.locked;
		}
		
		public final void setLocked(final boolean locked) {
			this.locked = locked;
		}
		
		public final T getObject() {
			return this.object;
		}
		
		private static final long serialVersionUID = 1490439068402247668L;
		
	}
	
}
