package imj3.draft.segmentation2;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public final class Classification<C extends ClassifierClass> implements Serializable {
	
	private final double[] input;
	
	private final C classifierClass;
	
	private final double score;
	
	public Classification(final double[] input, final C classifierClass,
			final double score) {
		this.input = input;
		this.classifierClass = classifierClass;
		this.score = score;
	}
	
	public final double[] getInput() {
		return this.input;
	}
	
	public final C getClassifierClass() {
		return this.classifierClass;
	}
	
	public final double getScore() {
		return this.score;
	}
	
	private static final long serialVersionUID = 4982888494257489470L;
	
}