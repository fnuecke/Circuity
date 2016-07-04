package li.cil.lib.synchronization.value;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.util.List;

@Serializable
public final class SynchronizedChar extends AbstractSynchronizedValue {
    @Serialize
    private char value;

    // --------------------------------------------------------------------- //

    public SynchronizedChar() {
    }

    public SynchronizedChar(final char value) {
        this.value = value;
    }

    // --------------------------------------------------------------------- //

    public char get() {
        return value;
    }

    public void set(final char value) {
        if (this.value != value) {
            this.value = value;
            setDirty();
        }
    }

    // --------------------------------------------------------------------- //
    // SynchronizedValue

    public void serialize(final PacketBuffer packet, @Nullable final List<Object> tokens) {
        packet.writeChar(value);
    }

    public void deserialize(final PacketBuffer packet) {
        value = packet.readChar();
    }
}
