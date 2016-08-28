package li.cil.lib.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.*;

public class RangeMapTest {
    @Test
    public void simple() throws Exception {
        final RangeMap<String> list = new RangeMap<>(1000);

        list.add("a", 0, 100);
        list.add("b", 100, 100);
        list.add("c", 900, 100);
        list.add("d", 500, 100);

        assertEquals("a", list.get(0));
        assertEquals("a", list.get(50));
        assertEquals("a", list.get(99));
        assertNotEquals("a", list.get(100));

        assertEquals("b", list.get(100));
        assertEquals("b", list.get(150));
        assertEquals("b", list.get(199));
        assertNotEquals("b", list.get(99));
        assertNotEquals("b", list.get(200));

        assertEquals("c", list.get(900));
        assertEquals("c", list.get(950));
        assertEquals("c", list.get(999));
        assertNotEquals("c", list.get(899));

        assertEquals("d", list.get(500));
        assertEquals("d", list.get(550));
        assertEquals("d", list.get(599));
        assertNotEquals("d", list.get(499));
        assertNotEquals("d", list.get(600));
    }

    private static final int RANGE_LENGTH = 0x10000;
    private static final int LOOKUPS_PER_ENTRY = 50;

    @Test
    public void stress() throws Exception {
        final RangeMap<String> list = new RangeMap<>(RANGE_LENGTH);

        final List<Entry> entries = new ArrayList<>();
        final Random rng = new Random(12345);

        // Fill list with random entries and keep track of them for reference.
        {
            final byte[] buffer = new byte[32];
            int offset = 0;
            while (offset < RANGE_LENGTH - 50) {
                if (rng.nextBoolean()) {
                    offset += 5 + rng.nextInt(25);
                }

                rng.nextBytes(buffer);
                final String value = UUID.nameUUIDFromBytes(buffer).toString();
                final int length = 10 + rng.nextInt(10);
                entries.add(new Entry(value, offset, length));
                list.add(value, offset, length);

                offset += length;
            }
        }

        // Pre-generate list of lookups (to avoid overhead during actual lookups).
        final List<Lookup> lookups = new ArrayList<>();
        for (final Entry entry : entries) {
            for (int i = 0; i < LOOKUPS_PER_ENTRY; i++) {
                final int offset = entry.offset + rng.nextInt(entry.length);
                lookups.add(new Lookup(entry.value, offset));
            }
        }

        // Run #1, grouped access.
        {
            final long start = System.currentTimeMillis();

            for (final Lookup lookup : lookups) {
                final String value = list.get(lookup.offset);
                assertEquals(value, lookup.value);
            }

            final long elapsed = System.currentTimeMillis() - start;
//            System.out.println("Grouped: " + elapsed + "ms");
        }

        // Run #2, random access (should be slower (and is)).
        if (false) {
            Collections.shuffle(lookups, rng);

            final long start = System.currentTimeMillis();

            for (final Lookup lookup : lookups) {
                final String value = list.get(lookup.offset);
                assertEquals(value, lookup.value);
            }

            final long elapsed = System.currentTimeMillis() - start;
//            System.out.println("Random: " + elapsed + "ms");
        }
    }

    private static final class Entry {
        public final String value;
        public final int offset;
        public final int length;

        private Entry(final String value, final int offset, final int length) {
            this.value = value;
            this.offset = offset;
            this.length = length;
        }
    }

    private static final class Lookup {
        public final String value;
        public final int offset;

        private Lookup(final String value, final int offset) {
            this.value = value;
            this.offset = offset;
        }
    }

    @Test
    public void remove() throws Exception {
        final RangeMap<String> list = new RangeMap<>(1000);

        list.add("a", 0, 100);
        list.add("b", 100, 100);

        assertEquals(2, list.size());

        assertTrue(list.remove("a"));
        assertFalse(list.remove("a"));

        list.remove(150);

        assertEquals(0, list.size());

        // Create entry that goes across both previous entries to check whether
        // removed entries were properly merged with adjacent null entries.
        list.add("c", 50, 100);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void boundsAddLow() throws Exception {
        final RangeMap<String> list = new RangeMap<>(1000);

        list.add("a", -1, 100);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void boundsAddHigh() throws Exception {
        final RangeMap<String> list = new RangeMap<>(1000);

        list.add("a", 1010, 100);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void boundsGetLow() throws Exception {
        final RangeMap<String> list = new RangeMap<>(1000);

        list.add("a", 0, 100);
        list.get(-10);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void boundsGetHigh() throws Exception {
        final RangeMap<String> list = new RangeMap<>(1000);

        list.add("a", 0, 100);
        list.get(1010);
    }

    @Test(expected = IllegalArgumentException.class)
    public void overlap() throws Exception {
        final RangeMap<String> list = new RangeMap<>(1000);

        list.add("a", 0, 100);
        list.add("b", 50, 100);
    }
}