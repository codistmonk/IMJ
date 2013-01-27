package imj;

import static imj.MathOperations.compute;
import static imj.MathOperations.BinaryOperator.MINUS;

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
	
	public static final Image filterRankConnectiviy4(final Image image, final int rank) {
		return new RankFiltering(image, rank, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_4).getResult();
	}
	
	public static final Image filterRankConnectiviy8(final Image image, final int rank) {
		return new RankFiltering(image, rank, BASIC_STRUCTURING_ELEMENT_FOR_CONNECTIVITY_8).getResult();
	}
	
	public static final Image erode4(final Image image) {
		return filterRankConnectiviy4(image, 0);
	}
	
	public static final Image erode8(final Image image) {
		return filterRankConnectiviy8(image, 0);
	}
	
	public static final Image dilate4(final Image image) {
		return filterRankConnectiviy4(image, -1);
	}
	
	public static final Image dilate8(final Image image) {
		return filterRankConnectiviy8(image, -1);
	}
	
	public static final Image close4(final Image image) {
		return erode4(dilate4(image));
	}
	
	public static final Image close8(final Image image) {
		return erode8(dilate8(image));
	}
	
	public static final Image open4(final Image image) {
		return dilate4(erode4(image));
	}
	
	public static final Image open8(final Image image) {
		return dilate8(erode8(image));
	}
	
	public static final Image edges4(final Image image) {
		return compute(dilate4(image), MINUS, erode4(image));
	}
	
	public static final Image edges8(final Image image) {
		return compute(dilate8(image), MINUS, erode8(image));
	}
	
}
