package imj;

import static imj.Labeling.NeighborhoodShape.CONNECTIVITY_4;
import static imj.MathOperations.compute;
import static imj.MathOperations.BinaryOperator.Predefined.MINUS;
import static imj.MathOperations.BinaryOperator.Predefined.PLUS;
import static java.lang.Math.ceil;
import imj.IMJTools.StatisticsSelector;
import imj.Labeling.NeighborhoodShape.Distance;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-01-27)
 */
public final class MorphologicalOperations {
	
	private MorphologicalOperations() {
		throw new IllegalInstantiationException();
	}
	
	public static final Image filterRank(final Image image, final int rank, final int[] connectivity) {
		return new RankFilter(image, rank, connectivity).getResult();
	}
	
	public static final Image filterRankConnectiviy4(final Image image, final int rank) {
		return filterRank(image, rank, StructuringElement.SIMPLE_CONNECTIVITY_4);
	}
	
	public static final Image filterRankConnectiviy8(final Image image, final int rank) {
		return filterRank(image, rank, StructuringElement.SIMPLE_CONNECTIVITY_8);
	}
	
	public static final Image erode(final Image image, final int[] connectivity) {
		return filterRank(image, 0, connectivity);
	}
	
	public static final Image erode4(final Image image) {
		return erode(image, StructuringElement.SIMPLE_CONNECTIVITY_4);
	}
	
	public static final Image erode8(final Image image) {
		return erode(image, StructuringElement.SIMPLE_CONNECTIVITY_8);
	}
	
	public static final Image dilate(final Image image, final int[] connectivity) {
		return filterRank(image, -1, connectivity);
	}
	
	public static final Image dilate4(final Image image) {
		return dilate(image, StructuringElement.SIMPLE_CONNECTIVITY_4);
	}
	
	public static final Image dilate8(final Image image) {
		return dilate(image, StructuringElement.SIMPLE_CONNECTIVITY_8);
	}
	
	public static final Image close(final Image image, final int[] connectivity) {
		return erode(dilate(image, connectivity), connectivity);
	}
	
	public static final Image close4(final Image image) {
		return close(image, StructuringElement.SIMPLE_CONNECTIVITY_4);
	}
	
	public static final Image close8(final Image image) {
		return close(image, StructuringElement.SIMPLE_CONNECTIVITY_8);
	}
	
	public static final Image open(final Image image, final int[] connectivity) {
		return dilate(erode(image, connectivity), connectivity);
	}
	
	public static final Image open4(final Image image) {
		return open(image, StructuringElement.SIMPLE_CONNECTIVITY_4);
	}
	
	public static final Image open8(final Image image) {
		return open(image, StructuringElement.SIMPLE_CONNECTIVITY_8);
	}
	
	public static final Image edges(final Image image, final int[] connectivity) {
		return new ValueStatisticsFilter(image, StatisticsSelector.AMPLITUDE, connectivity).getResult();
	}
	
	public static final Image edges4(final Image image) {
		return edges(image, StructuringElement.SIMPLE_CONNECTIVITY_4);
	}
	
	public static final Image edges8(final Image image) {
		return edges(image, StructuringElement.SIMPLE_CONNECTIVITY_8);
	}
	
	public static final Image hMinima(final Image image, final int h, final int[] connectivity) {
		return new ReconstructionByErosion(
				image,
				compute(PLUS.bindRight(0, h), image, new ImageOfInts(image.getRowCount(), image.getColumnCount(), image.getChannelCount())),
				connectivity
				).getResult();
	}
	
	public static final Image hMinima4(final Image image, final int h) {
		return hMinima(image, h, StructuringElement.SIMPLE_CONNECTIVITY_4);
	}
	
	public static final Image hMinima8(final Image image, final int h) {
		return hMinima(image, h, StructuringElement.SIMPLE_CONNECTIVITY_8);
	}
	
	public static final Image hMaxima(final Image image, final int h, final int[] connectivity) {
		return new ReconstructionByDilation(
				image,
				compute(MINUS.bindRight(0, h), image, new ImageOfInts(image.getRowCount(), image.getColumnCount(), image.getChannelCount())),
				CONNECTIVITY_4
				).getResult();
	}
	
	public static final Image hMaxima4(final Image image, final int h) {
		return hMaxima(image, h, StructuringElement.SIMPLE_CONNECTIVITY_4);
	}
	
	public static final Image hMaxima8(final Image image, final int h) {
		return hMaxima(image, h, StructuringElement.SIMPLE_CONNECTIVITY_8);
	}
	
	/**
	 * @author codistmonk (creation 2013-02-03)
	 */
	public static final class StructuringElement {
		
		private StructuringElement() {
			throw new IllegalInstantiationException();
		}
		
		public static final int[] SIMPLE_CONNECTIVITY_4 = newDisk(1.0, Distance.CITYBLOCK);
		
		public static final int[] SIMPLE_CONNECTIVITY_8 = newDisk(1.0, Distance.CHESSBOARD);
		
		public static final int[] newDisk(final double radius, final Distance distance) {
			final int r = (int) ceil(radius);
			final IntList resultBuilder = new IntList();
			
			for (int i = -r; i <= +r; ++i) {
				for (int j = -r; j <= +r; ++j) {
					if (distance.distance(0, 0, j, i) <= radius) {
						resultBuilder.add(i);
						resultBuilder.add(j);
					}
				}
			}
			
			return resultBuilder.toArray();
		}
		
		public static final int[] newDisk(final double radius) {
			return newDisk(radius, Distance.EUCLIDEAN);
		}
		
	}
	
}
