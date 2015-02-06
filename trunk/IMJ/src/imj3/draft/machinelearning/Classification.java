package imj3.draft.machinelearning;

import java.io.Serializable;

/**
 * @author codistmonk (creation 2015-02-04)
 */
public final class Classification<C extends ClassifierClass> implements Serializable {
	
	private double[] input;
	
	private C classifierClass;
	
	private double score;
	
	public Classification() {
		// NOP
	}
	
	public Classification(final double[] input, final C classifierClass, final double score) {
		this.input = input;
		this.classifierClass = classifierClass;
		this.score = score;
	}
	
	public final double[] getInput() {
		return this.input;
	}
	
	public final Classification<C> setInput(final double[] input) {
		this.input = input;
		
		return this;
	}
	
	public final C getClassifierClass() {
		return this.classifierClass;
	}
	
	public final Classification<C> setClassifierClass(final C classifierClass) {
		this.classifierClass = classifierClass;
		
		return this;
	}
	
	public final double getScore() {
		return this.score;
	}
	
	public final Classification<C> setScore(final double score) {
		this.score = score;
		
		return this;
	}
	
	private static final long serialVersionUID = 4982888494257489470L;
	
}
