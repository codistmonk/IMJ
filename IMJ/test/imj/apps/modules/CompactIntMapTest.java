package imj.apps.modules;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * @author codistmonk (creation 2013-04-24)
 */
public class CompactIntMapTest {
	
	@Test
	public final void test1() {
		final CompactIntMap map = new CompactIntMap();
		
		assertEquals(0, map.getElementCount());
		assertEquals(0, map.getChunkCount());
		assertEquals(null, map.get(0));
		assertEquals(null, map.get(1));
		assertEquals(null, map.get(2));
		assertEquals(null, map.get(3));
		assertEquals(null, map.get(4));
		assertEquals("[]", toStrings(map.entries()).toString());
		
		map.put(0, "B");
		
		assertEquals(1, map.getElementCount());
		assertEquals(1, map.getChunkCount());
		assertEquals("B", map.get(0));
		assertEquals(null, map.get(1));
		assertEquals(null, map.get(2));
		assertEquals(null, map.get(3));
		assertEquals(null, map.get(4));
		assertEquals("[0=B]", toStrings(map.entries()).toString());
		
		map.put(0, "A");
		
		assertEquals(1, map.getElementCount());
		assertEquals(1, map.getChunkCount());
		assertEquals("A", map.get(0));
		assertEquals(null, map.get(1));
		assertEquals(null, map.get(2));
		assertEquals(null, map.get(3));
		assertEquals(null, map.get(4));
		assertEquals("[0=A]", toStrings(map.entries()).toString());
		
		map.put(3, "D");
		
		assertEquals(2, map.getElementCount());
		assertEquals(2, map.getChunkCount());
		assertEquals("A", map.get(0));
		assertEquals(null, map.get(1));
		assertEquals(null, map.get(2));
		assertEquals("D", map.get(3));
		assertEquals(null, map.get(4));
		assertEquals("[0=A, 3=D]", toStrings(map.entries()).toString());
		
		map.put(4, "E");
		
		assertEquals(3, map.getElementCount());
		assertEquals(2, map.getChunkCount());
		assertEquals("A", map.get(0));
		assertEquals(null, map.get(1));
		assertEquals(null, map.get(2));
		assertEquals("D", map.get(3));
		assertEquals("E", map.get(4));
		assertEquals("[0=A, 3=D, 4=E]", toStrings(map.entries()).toString());
		
		map.put(2, "C");
		
		assertEquals(4, map.getElementCount());
		assertEquals(2, map.getChunkCount());
		assertEquals("A", map.get(0));
		assertEquals(null, map.get(1));
		assertEquals("C", map.get(2));
		assertEquals("D", map.get(3));
		assertEquals("E", map.get(4));
		assertEquals("[0=A, 2=C, 3=D, 4=E]", toStrings(map.entries()).toString());
		
		map.put(1, "B");
		
		assertEquals(5, map.getElementCount());
		assertEquals(1, map.getChunkCount());
		assertEquals("A", map.get(0));
		assertEquals("B", map.get(1));
		assertEquals("C", map.get(2));
		assertEquals("D", map.get(3));
		assertEquals("E", map.get(4));
		assertEquals("[0=A, 1=B, 2=C, 3=D, 4=E]", toStrings(map.entries()).toString());
	}
	
	public static final <E> List<String> toStrings(final Iterable<E> elements) {
		final List<String> result = new ArrayList<String>();
		
		for (final E element : elements) {
			result.add("" + element);
		}
		
		return result;
	}
	
}
