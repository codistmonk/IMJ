package imj2.core;

import static net.sourceforge.aprog.tools.Tools.unchecked;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.sourceforge.aprog.tools.IllegalInstantiationException;
import net.sourceforge.aprog.tools.SystemProperties;

/**
 * @author codistmonk (creation 2013-08-15)
 */
public final class MultiThreadTools {
	
	private MultiThreadTools() {
		throw new IllegalInstantiationException();
	}
	
	public static final int WORKER_COUNT = SystemProperties.getAvailableProcessorCount();
	
	private static final Map<Thread, Integer> workerIds = new HashMap<Thread, Integer>();
	
	private static ExecutorService executor;
	
	public static final synchronized ExecutorService getExecutor() {
		if (executor == null) {
			executor = Executors.newFixedThreadPool(WORKER_COUNT);
		}
		
		return executor;
	}
	
	public static final void wait(final Iterable<? extends Future<?>> tasks) {
		try {
			for (final Future<?> task : tasks) {
				task.get();
			}
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
	}
	
	public static final int getWorkerId() {
		return getOrCreateId(workerIds, Thread.currentThread());
	}
	
	public static final <K> int getOrCreateId(final Map<K, Integer> ids, final K key) {
		synchronized (ids) {
			Integer result = ids.get(key);
			
			if (result == null) {
				result = ids.size();
				ids.put(key, result);
			}
			
			return result;
		}
	}
	
}
