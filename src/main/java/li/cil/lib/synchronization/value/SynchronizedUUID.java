package li.cil.lib.synchronization.value;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Serializable
public final class SynchronizedUUID extends AbstractSynchronizedValue {
    @Serialize
    private UUID value;

    // --------------------------------------------------------------------- //

    public SynchronizedUUID() {
    }

    public SynchronizedUUID(@Nullable final UUID value) {
        this.value = value;
    }

    // --------------------------------------------------------------------- //

    @Nullable
    public UUID get() {
        return value;
    }

    public void set(@Nullable final UUID value) {
        if (!Objects.equals(this.value, value)) {
            this.value = value;
            setDirty();
        }
    }

    // --------------------------------------------------------------------- //
    // SynchronizedValue

    public void serialize(final PacketBuffer packet, @Nullable final List<Object> tokens) {
        if (value == null) {
            packet.writeBoolean(false);
        } else {
            packet.writeBoolean(true);
            packet.writeLong(value.getMostSignificantBits());
            packet.writeLong(value.getLeastSignificantBits());
        }
    }

    public void deserialize(final PacketBuffer packet) {
        if (packet.readBoolean()) {
            final long mostSigBits = packet.readLong();
            final long leastSigBits = packet.readLong();
            value = new UUID(mostSigBits, leastSigBits);
        } else {
            value = null;
        }
    }
}
