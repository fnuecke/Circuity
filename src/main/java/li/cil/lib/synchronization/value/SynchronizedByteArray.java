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
public final class SynchronizedByteArray implements SynchronizedValue {
    private final Object lock = new Object();

    @Serialize
    private byte[] value;

    private SynchronizationManagerServer manager;

    // --------------------------------------------------------------------- //

    public SynchronizedByteArray(final int initialCapacity) {
        value = new byte[initialCapacity];
    }

    public SynchronizedByteArray() {
        this(0);
    }

    public SynchronizedByteArray(final byte[] value) {
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

    public byte get(final int index) {
        synchronized (lock) {
            return value[index];
        }
    }

    public byte set(final int index, final byte element) {
        synchronized (lock) {
            if (value[index] != element) {
                setDirty(index);
                value[index] = element;
            }
        }
        return element;
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

            packet.writeByteArray(value);
        } else {
            packet.writeBoolean(false);

            final Set<Object> indices = new HashSet<>(tokens);
            packet.writeVarIntToBuffer(indices.size());

            for (final Object i : indices) {
                final int index = (Integer) i;
                packet.writeVarIntToBuffer(index);
                packet.writeByte(value[index]);
            }
        }
    }

    @Override
    public void deserialize(final PacketBuffer packet) {
        if (packet.readBoolean()) {
            value = packet.readByteArray();
        } else {
            final int count = packet.readVarIntFromBuffer();

            for (int i = 0; i < count; i++) {
                final int index = packet.readVarIntFromBuffer();
                value[index] = packet.readByte();
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
