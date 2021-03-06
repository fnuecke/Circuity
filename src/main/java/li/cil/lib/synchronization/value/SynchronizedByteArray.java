package li.cil.lib.synchronization.value;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;
import io.netty.buffer.Unpooled;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.api.synchronization.SynchronizationManagerServer;
import li.cil.lib.api.synchronization.SynchronizedValue;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

@Serializable
public final class SynchronizedByteArray implements SynchronizedValue {
    private static final PacketBuffer[] BUFFERS = new PacketBuffer[0x100];

    static {
        for (int i = 0; i < BUFFERS.length; i++) {
            BUFFERS[i] = new PacketBuffer(Unpooled.buffer());
        }
    }

    private final Object lock = new Object(); // To avoid changes to array during serialization.

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
        synchronized (lock) {
            value = Arrays.copyOf(value, size);
            setDirty(-1);
        }
    }

    public int size() {
        return value.length;
    }

    public byte get(final int index) {
        return value[index];
    }

    public byte set(final int index, final byte element) {
        if (value[index] != element) {
            synchronized (lock) {
                value[index] = element;
                setDirty(index);
            }
        }
        return element;
    }

    public void fill(final byte item) {
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
    public byte[] array() {
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
            if (tokens == null) {
                serializeFully(packet);
            } else {
                final TIntHashSet indices = (TIntHashSet) tokens.get(0);
                if (indices.contains(-1) || !serializeChanges(packet, indices)) {
                    serializeFully(packet);
                }
            }
        }
    }

    @Override
    public void deserialize(final PacketBuffer packet) {
        if (packet.readBoolean()) {
            deserializeFully(packet);
        } else {
            deserializeChanges(packet);
        }
    }

    // --------------------------------------------------------------------- //

    private void serializeFully(final PacketBuffer packet) {
        packet.writeBoolean(true);

        packet.writeByteArray(value);
    }

    private boolean serializeChanges(final PacketBuffer packet, final TIntHashSet indices) {
        packet.markWriterIndex();

        packet.writeBoolean(false);

        final int maxWriterIndex = packet.writerIndex() + value.length;

        synchronized (BUFFERS) {
            final TIntIterator it = indices.iterator();
            while (it.hasNext()) {
                final int index = it.next();
                BUFFERS[value[index] & 0xFF].writeVarInt(index);
            }

            for (int item = 0; item < BUFFERS.length; item++) {
                final PacketBuffer buffer = BUFFERS[item];
                if (buffer.isReadable()) {
                    if (packet.writerIndex() < maxWriterIndex) {
                        packet.writeByte(item);
                        packet.writeVarInt(buffer.writerIndex());
                        packet.writeBytes(buffer);
                    }
                    buffer.clear();
                }
            }
        }

        if (packet.writerIndex() >= maxWriterIndex) {
            packet.resetWriterIndex();
            return false;
        } else {
            return true;
        }
    }

    private void deserializeFully(final PacketBuffer packet) {
        value = packet.readByteArray();
    }

    private void deserializeChanges(final PacketBuffer packet) {
        while (packet.isReadable()) {
            final byte item = packet.readByte();
            final int length = packet.readVarInt();
            final int end = packet.readerIndex() + length;
            while (packet.readerIndex() < end) {
                final int index = packet.readVarInt();
                value[index] = item;
            }
        }
    }

    private void setDirty(final int index) {
        if (manager != null) {
            manager.setDirtyAdvanced(this, (tokens) -> {
                final TIntHashSet indices;
                if (tokens.size() == 0) {
                    tokens.add(indices = new TIntHashSet());
                } else {
                    indices = (TIntHashSet) tokens.get(0);
                }
                indices.add(index);
            });
        }
    }
}
