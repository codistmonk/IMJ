package imj;

import static imj.Labeling.CONNECTIVITY_4;
import static imj.MathOperations.compute;
import static imj.MathOperations.BinaryOperator.Predefined.MINUS;
import static imj.MathOperations.BinaryOperator.Predefined.PLUS;
import static java.lang.Math.max;
import static java.lang.Math.min;
import net.sourceforge.aprog.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-01-27)
 */
public final class MorphologyOperations {
	
	private MorphologyOperations() {
		throw new IllegalInstantiationException();
	}
	
	public static final int[] BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_4 = {
		-1, +0,
		+0, -1,
		+0, +0,
		+0, +1,
		+1, +0,
	};
	
	public static final int[] BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_8 = {
		-1, -1,
		-1, +0,
		-1, +1,
		+0, -1,
		+0, +0,
		+0, +1,
		+1, -1,
		+1, +0,
		+1, +1,
	};
	
	public static final Image filterRank(final Image image, final int rank, final int[] connectivity) {
		return new RankFiltering(image, rank, connectivity).getResult();
	}
	
	public static final Image filterRankConnectiviy4(final Image image, final int rank) {
		return filterRank(image, rank, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_4);
	}
	
	public static final Image filterRankConnectiviy8(final Image image, final int rank) {
		return filterRank(image, rank, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_8);
	}
	
	public static final Image erode(final Image image, final int[] connectivity) {
		return filterRank(image, 0, connectivity);
	}
	
	public static final Image erode4(final Image image) {
		return erode(image, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_4);
	}
	
	public static final Image erode8(final Image image) {
		return erode(image, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_8);
	}
	
	public static final Image dilate(final Image image, final int[] connectivity) {
		return filterRank(image, -1, connectivity);
	}
	
	public static final Image dilate4(final Image image) {
		return dilate(image, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_4);
	}
	
	public static final Image dilate8(final Image image) {
		return dilate(image, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_8);
	}
	
	public static final Image close(final Image image, final int[] connectivity) {
		return erode(dilate(image, connectivity), connectivity);
	}
	
	public static final Image close4(final Image image) {
		return close(image, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_4);
	}
	
	public static final Image close8(final Image image) {
		return close(image, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_8);
	}
	
	public static final Image open(final Image image, final int[] connectivity) {
		return dilate(erode(image, connectivity), connectivity);
	}
	
	public static final Image open4(final Image image) {
		return open(image, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_4);
	}
	
	public static final Image open8(final Image image) {
		return open(image, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_8);
	}
	
	public static final Image edges(final Image image, final int[] connectivity) {
		return compute(dilate(image, connectivity), MINUS, erode(image, connectivity));
	}
	
	public static final Image edges4(final Image image) {
		return edges(image, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_4);
	}
	
	public static final Image edges8(final Image image) {
		return edges(image, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_8);
	}
	
	public static final Image hMinima(final Image image, final int h, final int[] connectivity) {
		return new ReconstructionByErosion(
				image,
				compute(PLUS.bindRight(0, h), image, new ImageOfInts(image.getRowCount(), image.getColumnCount())),
				CONNECTIVITY_4
				).getResult();
	}
	
	public static final Image hMinima4(final Image image, final int h) {
		return hMinima(image, h, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_4);
	}
	
	public static final Image hMinima8(final Image image, final int h) {
		return hMinima(image, h, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_8);
	}
	
	public static final Image hMaxima(final Image image, final int h, final int[] connectivity) {
		return new ReconstructionByDilation(
				image,
				compute(MINUS.bindRight(0, h), image, new ImageOfInts(image.getRowCount(), image.getColumnCount())),
				CONNECTIVITY_4
				).getResult();
	}
	
	public static final Image hMaxima4(final Image image, final int h) {
		return hMaxima(image, h, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_4);
	}
	
	public static final Image hMaxima8(final Image image, final int h) {
		return hMaxima(image, h, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_8);
	}
	
}
