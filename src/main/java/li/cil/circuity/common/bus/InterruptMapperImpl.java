package li.cil.circuity.common.bus;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.controller.InterruptMapper;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.circuity.api.bus.device.InterruptSource;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.SerializationListener;
import li.cil.lib.api.serialization.Serialize;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The interrupt mapper is responsible of keeping track of which interrupt
 * sources are mapped to which interrupt sinks.
 * <p>
 * To that purpose, it assigns each interrupt source a locally unique source
 * ID, and each sink a locally unique sink ID. It then keeps a mapping of
 * source instance and source local interrupt to source IDs, from source IDs
 * to sink IDs (that being the actual configurable part), from sink ID to sink
 * instance and sink local interrupt.
 */
@Serializable
public class InterruptMapperImpl implements InterruptMapper, SerializationListener {
    /**
     * The controller hosting this system.
     */
    private final AbstractBusController controller;

    /**
     * The list of occupied interrupt source IDs. These IDs get assigned to
     * {@link InterruptSource}s as they are connected to the bus. Used to
     * quickly look up free IDs.
     */
    // No @Serialize, restored in post-deserialization hook.
    private final BitSet interruptSourceIds = new BitSet();

    /**
     * The list of occupied interrupt sink IDs. These IDs get assigned to
     * {@link InterruptSink}s as they are connected to the bus. Used to
     * quickly look up free IDs.
     */
    // No @Serialize, restored in post-deserialization hook.
    private final BitSet interruptSinkIds = new BitSet();

    /**
     * The mapping of source instance and source local index to source ID.
     */
    private final TObjectIntMap<InterruptInfo<InterruptSource>> infoToSourceId = new TObjectIntHashMap<>();
    @Serialize
    private final Map<UUID, int[]> persistentInfoToSourceId = new HashMap<>();

    /**
     * The mapping of sink ID to sink instance and sink local index.
     */
    private final TIntObjectMap<InterruptInfo<InterruptSink>> sinkIdToInfo = new TIntObjectHashMap<>();
    @Serialize
    private final Map<UUID, int[]> persistentSinkIdToInfo = new HashMap<>();

    /**
     * The mapping of source to sink interrupt IDs. A value of <code>-1</code>
     * means that there is no mapping for that source interrupt ID. This array
     * will be grown as necessary.
     */
    @SuppressWarnings("ZeroLengthArrayAllocation") // Avoids having to init with -1s.
    @Serialize
    private int[] sourceIdToSinkId = new int[0];

    // --------------------------------------------------------------------- //

    public InterruptMapperImpl(final AbstractBusController controller) {
        this.controller = controller;
    }

    // --------------------------------------------------------------------- //
    // InterruptMapper

    @Override
    public void setInterruptMapping(final int sourceId, final int sinkId) {
        if (sourceId < 0 || !interruptSourceIds.get(sourceId)) {
            throw new IllegalArgumentException("sourceId");
        }
        if (sinkId > 0 && !interruptSinkIds.get(sinkId)) {
            throw new IllegalArgumentException("sinkId");
        }
        sourceIdToSinkId[sourceId] = sinkId < 0 ? -1 : sinkId;
    }

    @Override
    public PrimitiveIterator.OfInt getInterruptSourceIds(final InterruptSource device) {
        return Arrays.stream(persistentInfoToSourceId.get(device.getPersistentId())).iterator();
    }

    @Override
    public PrimitiveIterator.OfInt getInterruptSinkIds(final InterruptSink device) {
        return Arrays.stream(persistentSinkIdToInfo.get(device.getPersistentId())).iterator();
    }

    @Override
    public void interrupt(final InterruptSource source, final int sourceInterrupt, final int data) {
        if (!controller.isOnline()) return;
        final int interruptSourceId = infoToSourceId.get(InterruptInfo.of(source, sourceInterrupt));
        final int interruptSinkId = sourceIdToSinkId[interruptSourceId];
        if (interruptSinkId >= 0) {
            final InterruptInfo<InterruptSink> sink = sinkIdToInfo.get(interruptSinkId);
            sink.instance.interrupt(sink.index, data);
        }
    }

    // --------------------------------------------------------------------- //
    // Subsystem

    @Override
    public void add(final BusElement device) {
        if (device instanceof InterruptSource) {
            final InterruptSource source = (InterruptSource) device;

            final int count = source.getEmittedInterrupts();
            final int[] ids;
            if (persistentInfoToSourceId.containsKey(source.getPersistentId())) {
                ids = persistentInfoToSourceId.get(source.getPersistentId());
            } else {
                ids = allocateInterruptIds(interruptSourceIds, count);
                persistentInfoToSourceId.put(source.getPersistentId(), ids);
            }
            for (int index = 0; index < ids.length; index++) {
                infoToSourceId.put(InterruptInfo.of(source, index), ids[index]);
            }

            // Ensure capacity of interrupt map is sufficiently large if new sources were added.
            if (sourceIdToSinkId.length < interruptSourceIds.length()) {
                final int oldLength = sourceIdToSinkId.length;
                sourceIdToSinkId = Arrays.copyOf(sourceIdToSinkId, interruptSourceIds.length());
                Arrays.fill(sourceIdToSinkId, oldLength, sourceIdToSinkId.length, -1);
            }
        }

        if (device instanceof InterruptSink) {
            final InterruptSink sink = (InterruptSink) device;

            final int count = sink.getAcceptedInterrupts();
            final int[] ids;
            if (persistentSinkIdToInfo.containsKey(sink.getPersistentId())) {
                ids = persistentSinkIdToInfo.get(sink.getPersistentId());
            } else {
                ids = allocateInterruptIds(interruptSinkIds, count);
                persistentSinkIdToInfo.put(sink.getPersistentId(), ids);
            }
            for (int index = 0; index < ids.length; index++) {
                sinkIdToInfo.put(ids[index], InterruptInfo.of(sink, index));
            }
        }
    }

    @Override
    public void remove(final BusElement element) {
        if (element instanceof InterruptSource) {
            final InterruptSource source = (InterruptSource) element;
            final int[] ids = persistentInfoToSourceId.get(source.getPersistentId());
            for (final int id : ids) {
                interruptSourceIds.clear(id);
                sourceIdToSinkId[id] = -1;
            }

            infoToSourceId.retainEntries((key, value) -> ObjectUtils.notEqual(key.instance, element));
            persistentInfoToSourceId.remove(source.getPersistentId());
        }

        if (element instanceof InterruptSink) {
            final InterruptSink sink = (InterruptSink) element;
            final int[] ids = persistentSinkIdToInfo.get(sink.getPersistentId());
            for (final int id : ids) {
                interruptSinkIds.clear(id);
                sinkIdToInfo.remove(id);
            }

            // Assumption: sourceIdToSinkId.length > ids.length, so iterating
            // it for each removed sink id would be less efficient.
            for (int sourceId = 0, end = interruptSourceIds.length(); sourceId < end; sourceId++) {
                final int sinkId = sourceIdToSinkId[sourceId];
                if (sinkId >= 0 && Arrays.binarySearch(ids, sinkId) >= 0) {
                    sourceIdToSinkId[sourceId] = -1;
                }
            }

            persistentSinkIdToInfo.remove(sink.getPersistentId());
        }
    }

    @Override
    public boolean validate() {
        {
            final Set<UUID> known = infoToSourceId.keySet().stream().
                    map(k -> k.instance.getPersistentId()).
                    collect(Collectors.toSet());

            final Iterator<Map.Entry<UUID, int[]>> it = persistentInfoToSourceId.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<UUID, int[]> entry = it.next();
                if (!known.contains(entry.getKey())) {
                    final int[] ids = entry.getValue();
                    for (final int id : ids) {
                        interruptSourceIds.clear(id);
                        sourceIdToSinkId[id] = -1;
                    }
                    it.remove();
                }
            }
        }

        {
            final Set<UUID> known = sinkIdToInfo.valueCollection().stream().
                    map(k -> k.instance.getPersistentId()).
                    collect(Collectors.toSet());

            final Iterator<Map.Entry<UUID, int[]>> it = persistentSinkIdToInfo.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<UUID, int[]> entry = it.next();
                if (!known.contains(entry.getKey())) {
                    final int[] ids = entry.getValue();
                    for (final int id : ids) {
                        interruptSinkIds.clear(id);
                    }

                    // Assumption: sourceIdToSinkId.length > ids.length, so iterating
                    // it for each removed sink id would be less efficient.
                    for (int sourceId = 0, end = interruptSourceIds.length(); sourceId < end; sourceId++) {
                        final int sinkId = sourceIdToSinkId[sourceId];
                        if (sinkId >= 0 && Arrays.binarySearch(ids, sinkId) >= 0) {
                            sourceIdToSinkId[sourceId] = -1;
                        }
                    }

                    it.remove();
                }
            }
        }

        return true;
    }

    @Override
    public void dispose() {
        interruptSourceIds.clear();
        interruptSinkIds.clear();
        infoToSourceId.clear();
        persistentInfoToSourceId.clear();
        sinkIdToInfo.clear();
        persistentSinkIdToInfo.clear();
        Arrays.fill(sourceIdToSinkId, -1);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void onBeforeSerialization() {
    }

    @Override
    public void onAfterSerialization() {
    }

    @Override
    public void onBeforeDeserialization() {
    }

    @Override
    public void onAfterDeserialization() {
        interruptSourceIds.clear();
        for (final int[] ids : persistentInfoToSourceId.values()) {
            for (final int id : ids) {
                interruptSourceIds.set(id);
            }
        }

        interruptSinkIds.clear();
        for (final int[] ids : persistentSinkIdToInfo.values()) {
            for (final int id : ids) {
                interruptSinkIds.set(id);
            }
        }
    }

    // --------------------------------------------------------------------- //

    private static int[] allocateInterruptIds(final BitSet set, final int count) {
        final int[] ids = new int[count];
        final BitSet notIds = new BitSet(set.length() + count);
        notIds.or(set);
        notIds.flip(0, notIds.length() + count);
        for (int id = notIds.nextSetBit(0), idx = 0; idx < count; id = notIds.nextSetBit(id + 1), idx++) {
            ids[idx] = id;
            if (id == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
        }
        return ids;
    }

    // --------------------------------------------------------------------- //

    private static final class InterruptInfo<T> {
        public final T instance;
        public final int index;

        private InterruptInfo(final T instance, final int index) {
            this.instance = instance;
            this.index = index;
        }

        @Override
        public int hashCode() {
            int result = instance.hashCode();
            result = 31 * result + index;
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final InterruptInfo<?> that = (InterruptInfo<?>) o;
            return index == that.index && instance.equals(that.instance);
        }

        @Override
        public String toString() {
            return instance.toString() + "#" + index;
        }

        public static <T> InterruptInfo<T> of(final T instance, final int index) {
            return new InterruptInfo<>(instance, index);
        }
    }
}
