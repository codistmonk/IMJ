package imj3.draft.machinelearning;

import static imj3.draft.machinelearning.Datum.Default.datum;
import static net.sourceforge.aprog.tools.MathTools.square;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public final class GaussianMixturePrototypeSource extends DataSource.Abstract<GaussianMixturePrototypeSource.Metadata> {
	
	private final int dimension;
	
	private final int size;
	
	public GaussianMixturePrototypeSource(final int n, final int dimension, final int size, final long seed) {
		super(new Metadata(n, dimension, seed));
		this.dimension = dimension;
		this.size = size;
	}
	
	@Override
	public final Iterator<Datum> iterator() {
		final int d = this.getInputDimension();
		final List<GaussianMixturePrototypeSource.Gaussian> gaussians = GaussianMixturePrototypeSource.this.getMetadata().getGaussians();
		
		return new Iterator<Datum>() {
			
			private final Random random = new Random(GaussianMixturePrototypeSource.this.getMetadata().getSeed());
			
			private final double[] datum = new double[d];
			
			private final Datum result = datum(this.datum);
			
			private int i = 0;
			
			@Override
			public final boolean hasNext() {
				return this.i < GaussianMixturePrototypeSource.this.size();
			}
			
			@Override
			public final Datum next() {
				++this.i;
				
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
	
	/**
	 * @author codistmonk (creation 2015-02-08)
	 */
	public static final class Metadata implements DataSource.Metadata {
		
		private final List<GaussianMixturePrototypeSource.Gaussian> gaussians;
		
		private final long seed;
		
		public Metadata(final int n, final int dimension, final long seed) {
			this.gaussians = new ArrayList<>(n);
			
			final Random random = new Random(seed);
			
			for (int i = 0; i < n; ++i) {
				this.gaussians.add(new Gaussian(random.doubles(dimension).toArray(), random.nextDouble()));
			}
			
			this.seed = random.nextLong();
		}
		
		public final List<GaussianMixturePrototypeSource.Gaussian> getGaussians() {
			return this.gaussians;
		}
		
		public final long getSeed() {
			return this.seed;
		}
		
		private static final long serialVersionUID = -8871661234430522022L;
		
	}
	
}
