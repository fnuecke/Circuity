package li.cil.lib.synchronization.value;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.util.List;

@Serializable
public final class SynchronizedShort extends AbstractSynchronizedValue {
    @Serialize
    private short value;

    // --------------------------------------------------------------------- //

    public SynchronizedShort() {
    }

    public SynchronizedShort(final short value) {
        this.value = value;
    }

    // --------------------------------------------------------------------- //

    public short get() {
        return value;
    }

    public void set(final short value) {
        if (this.value != value) {
            this.value = value;
            setDirty();
        }
    }

    // --------------------------------------------------------------------- //
    // SynchronizedValue

    public void serialize(final PacketBuffer packet, @Nullable final List<Object> tokens) {
        packet.writeShort(value);
    }

    public void deserialize(final PacketBuffer packet) {
        value = packet.readShort();
    }
}
