package imj2.tools;

import static java.lang.Thread.currentThread;
import static multij.tools.Tools.getOrCreate;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Map;
import java.util.WeakHashMap;

import multij.primitivelists.IntList;
import multij.tools.Factory.DefaultFactory;

/**
 * @author codistmonk (creation 2014-02-26)
 */
public final class SchedulingData implements Serializable {
	
	private final IntList todo;
	
	private final BitSet done;
	
	public SchedulingData() {
		this.todo = new IntList();
		this.done = new BitSet();
	}
	
	public final IntList getTodo() {
		return this.todo;
	}
	
	public final BitSet getDone() {
		return this.done;
	}
	
	/**
	 * {@value}.
	 */
	private static final long serialVersionUID = -3771171821850295230L;
	
	private static final Map<Thread, SchedulingData> reusableSchedulingData = new WeakHashMap<Thread, SchedulingData>();
	
	public static final DefaultFactory<SchedulingData> FACTORY = DefaultFactory.forClass(SchedulingData.class);
	
	public static final SchedulingData getInstance() {
		return getOrCreate(reusableSchedulingData, currentThread(), FACTORY);
	}
	
}
