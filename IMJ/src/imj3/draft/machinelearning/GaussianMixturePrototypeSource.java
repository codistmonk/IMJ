package imj3.draft.machinelearning;

import static net.sourceforge.aprog.tools.MathTools.square;

import imj3.draft.machinelearning.NearestNeighborClassifier.Prototype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public final class GaussianMixturePrototypeSource implements DataSource<Prototype> {
	
	private final int dimension;
	
	private final int size;
	
	private final List<GaussianMixturePrototypeSource.Gaussian> gaussians;
	
	private final long seed;
	
	public GaussianMixturePrototypeSource(final int n, final int dimension, final int size, final long seed) {
		this.dimension = dimension;
		this.size = size;
		this.gaussians = new ArrayList<>(n);
		
		final Random random = new Random(seed);
		
		for (int i = 0; i < n; ++i) {
			this.gaussians.add(new Gaussian(random.doubles(dimension).toArray(), random.nextDouble()));
		}
		
		this.seed = random.nextLong();
	}
	
	public final int getSize() {
		return this.size;
	}
	
	public final List<GaussianMixturePrototypeSource.Gaussian> getGaussians() {
		return this.gaussians;
	}
	
	public final long getSeed() {
		return this.seed;
	}
	
	@Override
	public final Iterator<Classification<Prototype>> iterator() {
		final int d = this.getInputDimension();
		
		return new Iterator<Classification<Prototype>>() {
			
			private final Random random = new Random(GaussianMixturePrototypeSource.this.getSeed());
			
			private final double[] datum = new double[d];
			
			private final Classification<Prototype> result = new Classification<>(
					this.datum, new Prototype(this.datum), 0.0);
			
			private int i = 0;
			
			@Override
			public final boolean hasNext() {
				return this.i < GaussianMixturePrototypeSource.this.size();
			}
			
			@Override
			public final Classification<Prototype> next() {
				++this.i;
				
				final List<GaussianMixturePrototypeSource.Gaussian> gaussians = GaussianMixturePrototypeSource.this.getGaussians();
				final GaussianMixturePrototypeSource.Gaussian gaussian = gaussians.get(this.random.nextInt(gaussians.size()));
				final double sigma2 = square(gaussian.getSigma());
				
				for (int i = 0; i < d; ++i) {
					this.datum[i] = gaussian.getCenter()[i] + this.random.nextGaussian() * sigma2;
				}
				
				return this.result;
			}
			
		};
	}
	
	@Override
	public final int getInputDimension() {
		return this.dimension;
	}
	
	@Override
	public final int getClassDimension() {
		return this.dimension;
	}
	
	@Override
	public final int size() {
		return this.size;
	}
	
	private static final long serialVersionUID = 3121457385669815840L;
	
	/**
	 * @author codistmonk (creation 2015-02-04)
	 */
	public static final class Gaussian implements Serializable {
		
		private final double[] center;
		
		private final double sigma;
		
		public Gaussian(final double[] center, final double sigma) {
			this.center = center;
			this.sigma = sigma;
		}
		
		public final double[] getCenter() {
			return this.center;
		}
		
		public final double getSigma() {
			return this.sigma;
		}
		
		private static final long serialVersionUID = 7955299362209802947L;
		
	}
	
}
