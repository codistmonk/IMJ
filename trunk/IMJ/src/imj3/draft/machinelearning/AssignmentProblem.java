package imj3.draft.machinelearning;

import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.util.stream.Collectors.toList;

import imj3.draft.machinelearning.GreedyAssociativeStreamingClustering.Association;
import imj3.draft.machinelearning.GreedyAssociativeStreamingClustering.EndPoint;
import imj3.draft.machinelearning.NearestNeighborClassifier.Prototype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import jgencode.primitivelists.DoubleList;

import net.sourceforge.aprog.tools.TicToc;
import net.sourceforge.aprog.tools.Tools;

/**
 * @author codistmonk (creation 2015-02-05)
 */
public final class AssignmentProblem {
	
	static final Random random = new Random(0L);
	
	/**
	 * @author codistmonk (creation 2015-02-05)
	 */
	public static final class LinearProgram implements Serializable {
		
		private double[] solution;
		
		private double[] objective;
		
		private final List<double[]> hyperplanes = new ArrayList<>();
		
		private final DoubleList hyperplaneOffsets = new DoubleList();
		
		private final List<double[]> halfHyperspaces = new ArrayList<>();
		
		private final DoubleList halfHyperspaceOffsets = new DoubleList();
		
		public final LinearProgram initialSolution(final double... initialSolution) {
			this.solution = initialSolution;
			
			return this;
		}
		
		public final LinearProgram objective(final double... objective) {
			this.objective = objective;
			
			return this;
		}
		
		public final LinearProgram hyperplane(final double... hyperplaneAndOffset) {
			final int n = hyperplaneAndOffset.length - 1;
			
			this.hyperplanes.add(Arrays.copyOf(hyperplaneAndOffset, n));
			this.hyperplaneOffsets.add(hyperplaneAndOffset[n]);
			
			return this;
		}
		
		public final LinearProgram halfHyperspace(final double... halfHyperspaceAndOffset) {
			final int n = halfHyperspaceAndOffset.length - 1;
			
			this.halfHyperspaces.add(Arrays.copyOf(halfHyperspaceAndOffset, n));
			this.halfHyperspaceOffsets.add(halfHyperspaceAndOffset[n]);
			
			return this;
		}
		
		public final double[] optimize() {
			final int precision = 8;
			final double[] objective = this.objective.clone();
			final int n = objective.length;
			final double[] tmp = new double[n];
			final List<double[]> constraints = copy(this.hyperplanes);
			
			constraints.addAll(copy(this.halfHyperspaces));
			
			final int hyperplaneCount = this.hyperplanes.size();
			int constraintCount = constraints.size();
			
			{
				for (int i = 0; i < hyperplaneCount; ++i) {
					System.arraycopy(constraints.get(i), 0, tmp, 0, n);
					
					project(objective, tmp);
					
					for (final double[] constraint : constraints) {
						project(constraint, tmp);
					}
					
					if (round(precision, dot(this.hyperplanes.get(i), this.solution)) != round(precision, this.hyperplaneOffsets.get(i))) {
						Tools.debugError();
						return null;
					}
				}
			}
			
			if (round(precision, dot(objective, this.objective)) < 0.0) {
				Tools.debugError();
				return this.solution;
			}
			
			for (int i = hyperplaneCount; i < constraintCount; ++i) {
				final double[] constraint = constraints.get(i);
				final double dcs = dot(constraint, this.solution);
				final double dco = dot(constraint, objective);
				
				if (round(precision, dcs) > round(precision, this.halfHyperspaceOffsets.get(i - hyperplaneCount))
						|| round(precision, dco) >= 0.0) {
					constraints.remove(i--);
					--constraintCount;
				}
			}
			
			for (int i = hyperplaneCount; i < constraintCount; ++i) {
				System.arraycopy(constraints.get(i), 0, tmp, 0, n);
				
				project(objective, tmp);
				
				for (int j = i; j < constraintCount; ++j) {
					project(constraints.get(j), tmp);
				}
			}
			
			{
				// (s + k obj) . c = off
				// s . c + k obj . c = off
				// k = (off - s . c) / obj . c
				double k = Double.POSITIVE_INFINITY;
				int i = -1;
				
				for (final double[] halfHyperspace : this.halfHyperspaces) {
					++i;
					final double doh = dot(objective, halfHyperspace);
					
					if (round(precision, doh) < 0.0) {
						k = min(k, (this.halfHyperspaceOffsets.get(i) - dot(this.solution, halfHyperspace)) / doh);
					}
				}
				
				if (0 < k && Double.isFinite(k)) {
					StreamingClustering.mergeInto(this.solution, 1.0, objective, k);
				}
			}
			
			// TODO repeat
			
			return this.solution;
		}
		
		private static final long serialVersionUID = 6331295733725395587L;
		
		public static final List<double[]> copy(final List<double[]> list) {
			return list.stream().map((Function<double[], double[]>) double[]::clone).collect(toList());
		}
		
	}
	
	public static final double round(final int digits, final double value) {
		final double scale = pow(10.0, digits);
		
		return Math.round(value * scale) / scale;
	}
	
	public static final double[] round(final int digits, final double[] values) {
		final int n = values.length;
		
		for (int i = 0; i < n; ++i) {
			values[i] = round(digits, values[i]);
		}
		
		return values;
	}
	
	/**
	 * @author codistmonk (creation 2015-02-06)
	 */
	public static final class SimplexAssociativeStreamingClustering extends NearestNeighborClustering {
		
		public SimplexAssociativeStreamingClustering(final Measure measure, final int clusterCount) {
			super(measure, clusterCount);
		}
		
		@Override
		protected final void cluster(final DataSource<?, Prototype> inputs,
				final NearestNeighborClassifier classifier) {
			// TODO Auto-generated method stub
			
		}
		
		private static final long serialVersionUID = -39181974892348593L;
		
	}
	
	/**
	 * @param commandLineArguments
	 * <br>Unused
	 */
	public static final void main(final String[] commandLineArguments) {
		if (true) {
			final LinearProgram lap = new LinearProgram();
			
			lap.initialSolution(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0);
//			lap.initialSolution(0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0);
			lap.objective(-250, -400, -200, -400, -600, -400, -350, -350, -250);
			lap.hyperplane(1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0);
			lap.hyperplane(0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 1.0);
			lap.hyperplane(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0);
			lap.hyperplane(1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0);
			lap.hyperplane(0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0);
			lap.hyperplane(0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0);
			
			lap.halfHyperspace(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
			lap.halfHyperspace(0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
			lap.halfHyperspace(0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
			lap.halfHyperspace(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
			lap.halfHyperspace(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0);
			lap.halfHyperspace(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0);
			lap.halfHyperspace(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0);
			lap.halfHyperspace(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0);
			lap.halfHyperspace(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0);
			
			final TicToc timer = new TicToc();
			final double[] solution = lap.optimize();
			final long t = timer.toc();
			Tools.debugPrint(Arrays.toString(round(1, solution)), t);
			
			return;
		}
		
		final int n = 10;
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
	
	public static final void project(final double[] v, final double[] direction) {
		// (v + k d) . d = 0
		// <- v . d + k d . d = 0
		// <- k = - v . d / d . d
		final double numerator = dot(v, direction);
		final double denominator = dot(direction, direction);
		final int n = v.length;
		
		for (int i = 0; i < n; ++i) {
			v[i] = v[i] - direction[i] * numerator / denominator;
		}
	}
	
	public static final double dot(final double[] v1, final double[] v2) {
		final int n = v1.length;
		double result = 0.0;
		
		for (int i = 0; i < n; ++i) {
			result += v1[i] * v2[i];
		}
		
		return result;
	}
	
	public static final double[] v(final double... v) {
		return v;
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
				association.getSource().setLocked(true);
				association.getTarget().setLocked(true);
				final boolean b = findBestAssignment(associations, bestAssociation, bestCost,
						currentAssociation, currentCost, nextI, timer, limit);
				association.getSource().setLocked(false);
				association.getTarget().setLocked(false);
				
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
	
}
