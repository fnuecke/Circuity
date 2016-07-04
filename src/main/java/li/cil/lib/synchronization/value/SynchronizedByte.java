package li.cil.lib.synchronization.value;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.util.List;

@Serializable
public final class SynchronizedByte extends AbstractSynchronizedValue {
    @Serialize
    private byte value;

    // --------------------------------------------------------------------- //

    public SynchronizedByte() {
    }

    public SynchronizedByte(final byte value) {
        this.value = value;
    }

    // --------------------------------------------------------------------- //

    public byte get() {
        return value;
    }

    public void set(final byte value) {
        if (this.value != value) {
            this.value = value;
            setDirty();
        }
    }

    // --------------------------------------------------------------------- //
    // SynchronizedValue

    public void serialize(final PacketBuffer packet, @Nullable final List<Object> tokens) {
        packet.writeByte(value);
    }

    public void deserialize(final PacketBuffer packet) {
        value = packet.readByte();
    }
}
