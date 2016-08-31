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
        value = Arrays.copyOf(value, size);
        setDirty(-1);
    }

    public int size() {
        return value.length;
    }

    public int get(final int index) {
        return value[index];
    }

    public int set(final int index, final int element) {
        if (value[index] != element) {
            setDirty(index);
            value[index] = element;
        }
        return element;
    }

    public int[] array() {
        return value;
    }

    public void setDirty() {
        setDirty(-1);
    }

    // --------------------------------------------------------------------- //
    // SynchronizedValue

    public void setManager(@Nullable final SynchronizationManagerServer manager) {
        this.manager = manager;
    }

    @Override
    public void serialize(final PacketBuffer packet, @Nullable final List<Object> tokens) {
        if (tokens == null || tokens.contains(-1)) {
            packet.writeBoolean(true);

            packet.writeVarIntArray(value);
        } else {
            packet.writeBoolean(false);

            final Set<Object> indices = new HashSet<>(tokens);
            packet.writeVarIntToBuffer(indices.size());

            for (final Object i : indices) {
                final int index = (Integer) i;
                packet.writeVarIntToBuffer(index);
                packet.writeVarIntToBuffer(value[index]);
            }
        }
    }

    @Override
    public void deserialize(final PacketBuffer packet) {
        if (packet.readBoolean()) {
            value = packet.readVarIntArray();
        } else {
            final int count = packet.readVarIntFromBuffer();

            for (int i = 0; i < count; i++) {
                final int index = packet.readVarIntFromBuffer();
                value[index] = packet.readVarIntFromBuffer();
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
