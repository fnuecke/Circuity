package li.cil.lib.synchronization.value;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.util.List;

@Serializable
public final class SynchronizedFloat extends AbstractSynchronizedValue {
    @Serialize
    private float value;

    // --------------------------------------------------------------------- //

    public SynchronizedFloat() {
    }

    public SynchronizedFloat(final float value) {
        this.value = value;
    }

    // --------------------------------------------------------------------- //

    public float get() {
        return value;
    }

    public void set(final float value) {
        if (this.value != value) {
            this.value = value;
            setDirty();
        }
    }

    // --------------------------------------------------------------------- //
    // SynchronizedValue

    public void serialize(final PacketBuffer packet, @Nullable final List<Object> tokens) {
        packet.writeFloat(value);
    }

    public void deserialize(final PacketBuffer packet) {
        value = packet.readFloat();
    }
}
