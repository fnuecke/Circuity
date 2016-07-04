package li.cil.lib.synchronization.value;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.util.List;

@Serializable
public final class SynchronizedDouble extends AbstractSynchronizedValue {
    @Serialize
    private double value;

    // --------------------------------------------------------------------- //

    public SynchronizedDouble() {
    }

    public SynchronizedDouble(final double value) {
        this.value = value;
    }

    // --------------------------------------------------------------------- //

    public double get() {
        return value;
    }

    public void set(final double value) {
        if (this.value != value) {
            this.value = value;
            setDirty();
        }
    }

    // --------------------------------------------------------------------- //
    // SynchronizedValue

    public void serialize(final PacketBuffer packet, @Nullable final List<Object> tokens) {
        packet.writeDouble(value);
    }

    public void deserialize(final PacketBuffer packet) {
        value = packet.readDouble();
    }
}
