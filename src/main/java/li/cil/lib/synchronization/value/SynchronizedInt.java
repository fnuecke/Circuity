package li.cil.lib.synchronization.value;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.util.List;

@Serializable
public final class SynchronizedInt extends AbstractSynchronizedValue {
    @Serialize
    private int value;

    // --------------------------------------------------------------------- //

    public SynchronizedInt() {
    }

    public SynchronizedInt(final int value) {
        this.value = value;
    }

    // --------------------------------------------------------------------- //

    public int get() {
        return value;
    }

    public void set(final int value) {
        if (this.value != value) {
            this.value = value;
            setDirty();
        }
    }

    // --------------------------------------------------------------------- //
    // SynchronizedValue

    public void serialize(final PacketBuffer packet, @Nullable final List<Object> tokens) {
        packet.writeInt(value);
    }

    public void deserialize(final PacketBuffer packet) {
        value = packet.readInt();
    }
}
