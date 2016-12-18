package li.cil.lib.synchronization.value;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.api.synchronization.SynchronizationManagerServer;
import li.cil.lib.api.synchronization.SynchronizedValue;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Serializable
public final class SynchronizedIntArray implements SynchronizedValue {
    private final Object lock = new Object(); // To avoid changes to array during serialization.

    @Serialize
    private int[] value;

    private SynchronizationManagerServer manager;

    // --------------------------------------------------------------------- //

    public SynchronizedIntArray(final int initialCapacity) {
        value = new int[initialCapacity];
    }

    public SynchronizedIntArray() {
        this(0);
    }

    public SynchronizedIntArray(final int[] value) {
        this.value = value.clone();
    }

    // --------------------------------------------------------------------- //

    public void setSize(final int size) {
        synchronized (lock) {
            value = Arrays.copyOf(value, size);
            setDirty(-1);
        }
    }

    public int size() {
        return value.length;
    }

    public int get(final int index) {
        return value[index];
    }

    public int set(final int index, final int element) {
        if (value[index] != element) {
            synchronized (lock) {
                value[index] = element;
                setDirty(index);
            }
        }
        return element;
    }

    public void fill(final int item) {
        synchronized (lock) {
            Arrays.fill(value, item);
            setDirty(-1);
        }
    }

    /**
     * Get the raw underlying array.
     * <p>
     * This is exposed <em>purely for reading</em>, for performance sensitive
     * use-cases (e.g. iterating the full array). Again, do <em>not</em>
     * write to this, as changes may lead to synchronization bugs.
     *
     * @return the underlying array.
     */
    public int[] array() {
        return value;
    }

    // --------------------------------------------------------------------- //
    // SynchronizedValue

    public void setManager(@Nullable final SynchronizationManagerServer manager) {
        this.manager = manager;
    }

    @Override
    public void serialize(final PacketBuffer packet, @Nullable final List<Object> tokens) {
        synchronized (lock) {
            if (tokens == null || tokens.contains(-1)) {
                packet.writeBoolean(true);

                packet.writeVarIntArray(value);
            } else {
                packet.writeBoolean(false);

                final Set<Object> indices = new HashSet<>(tokens);
                packet.writeVarInt(indices.size());

                for (final Object i : indices) {
                    final int index = (Integer) i;
                    packet.writeVarInt(index);
                    packet.writeVarInt(value[index]);
                }
            }
        }
    }

    @Override
    public void deserialize(final PacketBuffer packet) {
        if (packet.readBoolean()) {
            value = packet.readVarIntArray();
        } else {
            final int count = packet.readVarInt();

            for (int i = 0; i < count; i++) {
                final int index = packet.readVarInt();
                value[index] = packet.readVarInt();
            }
        }
    }

    // --------------------------------------------------------------------- //

    private void setDirty(final int index) {
        if (manager != null) {
            manager.setDirty(this, index);
        }
    }
}
