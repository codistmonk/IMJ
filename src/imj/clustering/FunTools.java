package imj.clustering;

import static java.lang.System.nanoTime;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static multij.tools.SystemProperties.getAvailableProcessorCount;
import static multij.tools.Tools.unchecked;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import multij.tools.IllegalInstantiationException;

/**
 * @author codistmonk (creation 2013-04-05)
 */
public final class FunTools {
	
	private FunTools() {
		throw new IllegalInstantiationException();
	}
	
	public static final int EXECUTOR_THREAD_COUNT = getAvailableProcessorCount();
	
	/**
	 * {@value}.
	 */
	public static final long EXECUTOR_MILLISECONDS_TO_LIVE = 4000L;
	
	private static ExecutorService executor;
	
	private static long executorExpirationDate;
	
	public static final synchronized ExecutorService getExecutor() {
		executorExpirationDate = nanoTime() + 1000000L * EXECUTOR_MILLISECONDS_TO_LIVE;
		
		if (executor == null || executor.isShutdown()) {
			executor = newFixedThreadPool(EXECUTOR_THREAD_COUNT);
			
			newExecutorKiller();
		}
		
		return executor;
	}
	
	public static final synchronized boolean maybeShutdownExecutor() {
		if (executor != null && executorExpirationDate < nanoTime()) {
			executor.shutdown();
			
			return true;
		}
		
		return false;
	}
	
	public static final <V> Callable<V> callable(final RangeTask<V> task, final int start, final int end) {
		return new Callable<V>() {
			
			@Override
			public final V call() throws Exception {
				return task.processRange(start, end);
			}
			
		};
	}
	
	public static final <T> List<Future<T>> schedule(final int n, final RangeTask<T> task, final List<Future<T>> result) {
		final ExecutorService executor = getExecutor();
		final List<Future<T>> actualResult = result != null ? result : new ArrayList<Future<T>>(EXECUTOR_THREAD_COUNT);
		
		if (n <= EXECUTOR_THREAD_COUNT) {
			for (int start = 0, end = 1; start < n; start = end, ++end) {
				actualResult.add(executor.submit(callable(task, start, end)));
			}
		} else {
			final int shortRange = n / EXECUTOR_THREAD_COUNT;
			final int longRange = shortRange + 1;
			final int longRangeCount = n % EXECUTOR_THREAD_COUNT;
			final int shortRangeCount = EXECUTOR_THREAD_COUNT - longRangeCount;
			
			for (int i = 0, start = 0, end = longRange; i < longRangeCount; ++i, start = end, end += longRange) {
				actualResult.add(executor.submit(callable(task, start, end)));
			}
			
			for (int i = 0, start = 0, end = shortRange; i < shortRangeCount; ++i, start = end, end += shortRange) {
				actualResult.add(executor.submit(callable(task, start, end)));
			}
		}
		
		return actualResult;
	}
	
	public static final <E> void removeAll(final List<E> elements, final Filter<E> filter) {
		for (int i = 0, end = elements.size(); i < end; ++i) {
			final E element = elements.get(i);
			
			if (filter.accept(element)) {
				final E last = elements.remove(--end);
				
				if (i < end) {
					elements.set(i--, last);
				}
			}
		}
	}
	
	public static final <E extends Comparable<E>> E parallelSmallest(final List<E> elements) {
		return parallelReduce(elements, new Smallest<E>());
	}
	
	public static final <E> E parallelReduce(final List<E> elements, final Reduce<E> reduce) {
		final List<Future<E>> tasks = schedule(elements.size(), new RangeTask<E>() {
			
			@Override
			public final E processRange(final int start, final int end) {
				return reduce(elements.subList(start, end), reduce);
			}
			
		}, null);
		
		if (tasks.isEmpty()) {
			return null;
		}
		
		E result;
		
		try {
			final Iterator<Future<E>> i = tasks.iterator();
			result = i.next().get();
			
			while (i.hasNext()) {
				result = reduce.reduce(i.next().get(), result);
			}
		} catch (final Exception exception) {
			throw unchecked(exception);
		}
		
		return result;
	}
	
	public static final <E extends Comparable<E>> E smallest(final Iterable<E> elements) {
		return reduce(elements, new Smallest<E>());
	}
	
	public static final <E> E reduce(final Iterable<E> elements, final Reduce<E> reduce) {
		final Iterator<E> i = elements.iterator();
		
		if (!i.hasNext()) {
			return null;
		}
		
		E result = i.next();
		
		while (i.hasNext()) {
			result = reduce.reduce(i.next(), result);
		}
		
		return result;
	}
	
	private static final Timer newExecutorKiller() {
		final Timer result = new Timer();
		
		result.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public final void run() {
				if (maybeShutdownExecutor()) {
					result.cancel();
				}
			}
			
		}, EXECUTOR_MILLISECONDS_TO_LIVE, EXECUTOR_MILLISECONDS_TO_LIVE);
		
		return result;
	}
	
	/**
	 * @author codistmonk (creation 2013-04-05)
	 *
	 * @param <V>
	 */
	public static abstract interface RangeTask<V> {
		
		public abstract V processRange(int start, int end);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-02)
	 *
	 * @param <T>
	 */
	public static abstract interface Filter<T> {
		
		public abstract boolean accept(T object);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-05)
	 *
	 * @param <E>
	 */
	public static abstract interface Reduce<E> {
		
		public abstract E reduce(E e1, E e2);
		
	}
	
	/**
	 * @author codistmonk (creation 2013-04-05)
	 *
	 * @param <E>
	 */
	public static final class Smallest<E extends Comparable<E>> implements Reduce<E> {
		
		@Override
		public final E reduce(final E e1, final E e2) {
			return e1.compareTo(e2) < 0 ? e1 : e2;
		}
		
	}
	
}
