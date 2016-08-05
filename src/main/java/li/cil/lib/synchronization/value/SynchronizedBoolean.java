package li.cil.lib.synchronization.value;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.util.List;

@Serializable
public final class SynchronizedBoolean extends AbstractSynchronizedValue {
    @Serialize
    private boolean value;

    // --------------------------------------------------------------------- //

    public SynchronizedBoolean() {
    }

    public SynchronizedBoolean(final boolean value) {
        this.value = value;
    }

    // --------------------------------------------------------------------- //

    public boolean get() {
        return value;
    }

    public void set(final boolean value) {
        if (this.value != value) {
            this.value = value;
            setDirty();
        }
    }

    // --------------------------------------------------------------------- //
    // SynchronizedValue

    public void serialize(final PacketBuffer packet, @Nullable final List<Object> tokens) {
        packet.writeBoolean(value);
    }

    public void deserialize(final PacketBuffer packet) {
        value = packet.readBoolean();
    }
}
