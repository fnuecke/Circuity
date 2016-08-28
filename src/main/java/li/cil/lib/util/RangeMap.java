package li.cil.lib.util;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * This is a simple mapping of intervals to values.
 * <p>
 * Overlapping intervals are not supported.
 * <p>
 * It heavily relies on the underlying heuristic for good performance, that
 * being that access tends to be clustered. Whenever a point is queried, the
 * interval containing it is moved to the front, making subsequent queries to
 * it much faster.
 *
 * @param <T> the type of value associated with each interval.
 */
public final class RangeMap<T> {
    /**
     * The total size of the range.
     * <p>
     * A length is required to maintain "null"-ranges, i.e. internal ranges
     * that are generated for unmapped areas. These are useful for improved
     * performance when accessing unmapped areas (as otherwise a full iteration
     * over all entries would be required for that).
     */
    private final long length;

    /**
     * The number of "null" entries, i.e. empty intervals.
     * <p>
     * Used for a fast {@link #size()} implementation.
     */
    private int nullEntries = 0;

    /**
     * The list of known intervals.
     * <p>
     * Note: LinkedList appears to be the more obvious candidate due to how
     * this list is used (linear iteration, then moving found value to front).
     * However, in speed tests this performed better. I'm assuming that this is
     * more cache-friendly than the linked list approach. Might be worth trying
     * a custom linked list implementation that pre-allocated entry nodes in
     * one block for cache-friendliness.
     */
    private final List<Entry<T>> children = new ArrayList<>();

    /**
     * Constructs a new range map with the specified length.
     * <p>
     * The map cannot contain intervals outside the specified length.
     *
     * @param length the length of the map.
     */
    public RangeMap(final long length) {
        this.length = length;
        clear();
    }

    /**
     * The number of intervals in the map.
     *
     * @return the number of intervals in the map.
     */
    public int size() {
        // That being the number of *added* intervals, so ignore null intervals.
        return children.size() - nullEntries;
    }

    /**
     * Get the value stored at the specified offset.
     * <p>
     * Returns <code>null</code> if no value is stored at that offset.
     *
     * @param offset the offset to get the value at.
     * @return the value stored at that offset, or <code>null</code>.
     */
    @Nullable
    public T get(final long offset) {
        return getChildAt(offset).value;
    }

    /**
     * Add a value to the map at the specified interval.
     *
     * @param value  the value to store.
     * @param offset the offset of the interval to store the value at.
     * @param length the length of the interval to store the value at.
     */
    public void add(final T value, final long offset, final long length) {
        final Entry<T> child = getChildAt(offset);

        // Can only add in null-entries.
        if (!child.permitsAdd(offset, length)) {
            throw new IllegalArgumentException("value overlaps existing item");
        }
        children.remove(child);
        --nullEntries;

        // Add new value.
        children.add(new Entry<>(offset, length, value));

        // Compute remaining interval left and right of added range.
        final long leftOffset = child.offset;
        final long leftLength = offset - leftOffset;
        final long rightOffset = offset + length;
        final long rightLength = child.offset - rightOffset + child.length;

        // Add padding null ranges resulting from split.
        if (leftLength > 0) {
            children.add(new Entry<>(leftOffset, leftLength, null));
            ++nullEntries;
        }
        if (rightLength > 0) {
            children.add(new Entry<>(rightOffset, rightLength, null));
            ++nullEntries;
        }
    }

    /**
     * Removes the specified value from the map.
     * <p>
     * Note that this runs in O(n), as it has to iterate all entries to find
     * the specified element. When the offset is known, prefer {@link #remove(long)}.
     *
     * @param value the value to remove.
     * @return <code>true</code> if the value was removed; <code>false</code> if it was not found.
     */
    public boolean remove(final T value) {
        for (int index = 0; index < children.size(); index++) {
            final Entry<T> child = children.get(index);
            if (Objects.equals(child.value, value)) {
                children.set(index, children.get(0));
                children.set(0, child);
                removeFirst(child);
                return true;
            }
        }
        return false;
    }

    /**
     * Remove the value at the specified offset from the map.
     * <p>
     * Note that the offset may be any value in the range associated to the
     * value to remove. It uses the same lookup logic as {@link #get(long)}.
     *
     * @param offset the offset at which to remove a value.
     * @return <code>true</code> if a value was removed; <code>false</code> otherwise.
     */
    public boolean remove(final long offset) {
        final Entry<T> entry = getChildAt(offset);
        if (entry.value != null) {
            removeFirst(entry);
            return true;
        }
        return false;
    }

    /**
     * Clears the map, removing all values from it.
     */
    public void clear() {
        children.clear();
        children.add(new Entry<>(0, length, null));
        nullEntries = 1;
    }

    // --------------------------------------------------------------------- //

    /**
     * Finds the entry containing the specified offset and moves it to the
     * front of the {@link #children} list.
     *
     * @param offset the offset to get the entry for.
     * @return the entry containing the specified offset.
     */
    private Entry<T> getChildAt(final long offset) {
        // If we have a result, move it to the front of the list. The heuristic
        // here is that multiple accesses will typically happen in close
        // proximity. I.e. the next access will probably be close to this one.
        // So moving this to the front will reduce the next search time.
        int index = 0;
        Entry<T> previous = children.get(index);
        for (; ; ) {
            if (previous.contains(offset)) {
                children.set(0, previous);
                return previous;
            }
            if (++index >= children.size()) {
                break;
            }
            final Entry<T> child = children.get(index);
            children.set(index, previous);
            previous = child;
        }

        // We have full coverage of our range via our children (including null
        // ranges), so this means we got an index that is out of our bounds.
        children.set(0, previous); // But avoid corrupting the list.
        throw new IndexOutOfBoundsException();
    }

    /**
     * Remove the entry that is currently at the front of the {@link #children}
     * list. The value itself is passed for convenience and readability.
     *
     * @param entry the value to remove, which must be at the front of the list.
     */
    private void removeFirst(final Entry<T> entry) {
        assert children.get(0) == entry;

        children.set(0, children.get(children.size() - 1));
        children.remove(children.size() - 1);

        long offset = entry.offset;
        long length = entry.length;

        // Try to find adjacent null ranges and merge with those.
        final Iterator<Entry<T>> it = children.iterator();
        int candidates = 2;
        while (it.hasNext()) {
            final Entry<T> other = it.next();

            // Check left.
            if (other.offset == offset - other.length) {
                if (other.value == null) {
                    it.remove();
                    --nullEntries;

                    offset = other.offset;
                    length += other.length;
                }

                if (--candidates == 0) {
                    break;
                }
            }

            // Check right.
            else if (offset == other.offset - length) {
                if (other.value == null) {
                    it.remove();
                    --nullEntries;

                    length += other.length;
                }

                if (--candidates == 0) {
                    break;
                }
            }
        }

        children.add(new Entry<>(offset, length, null));
        ++nullEntries;
    }

    // --------------------------------------------------------------------- //

    private static final class Entry<T> {
        public final long offset;
        public final long length;
        public final T value;

        public Entry(final long offset, final long length, @Nullable final T value) {
            this.offset = offset;
            this.length = length;
            this.value = value;
        }

        public boolean contains(final long offset) {
            return offset >= this.offset && offset - this.offset < length;
        }

        public boolean permitsAdd(final long offset, final long length) {
            return value == null && contains(offset) && contains(offset + length - 1);
        }
    }
}
