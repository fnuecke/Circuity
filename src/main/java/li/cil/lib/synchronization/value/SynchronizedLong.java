package li.cil.lib.synchronization.value;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.util.List;

@Serializable
public final class SynchronizedLong extends AbstractSynchronizedValue {
    @Serialize
    private long value;

    // --------------------------------------------------------------------- //

    public SynchronizedLong() {
    }

    public SynchronizedLong(final long value) {
        this.value = value;
    }

    // --------------------------------------------------------------------- //

    public long get() {
        return value;
    }

    public void set(final long value) {
        if (this.value != value) {
            this.value = value;
            setDirty();
        }
    }

    // --------------------------------------------------------------------- //
    // SynchronizedValue

    public void serialize(final PacketBuffer packet, @Nullable final List<Object> tokens) {
        packet.writeLong(value);
    }

    public void deserialize(final PacketBuffer packet) {
        value = packet.readLong();
    }
}
