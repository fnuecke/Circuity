package li.cil.lib.synchronization.value;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.network.PacketBuffer;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nullable;
import java.util.List;

@Serializable
public final class SynchronizedString extends AbstractSynchronizedValue {
    /**
     * The maximum supported length of strings that can be synchronized.
     */
    public static final int MAX_LENGTH = 8192;

    // --------------------------------------------------------------------- //

    @Serialize
    private String value;

    // --------------------------------------------------------------------- //

    public SynchronizedString() {
    }

    public SynchronizedString(final String value) {
        this.value = value;
    }

    // --------------------------------------------------------------------- //

    public String get() {
        return value;
    }

    public void set(final String value) {
        if (ObjectUtils.notEqual(this.value, value)) {
            if (value.length() > MAX_LENGTH) {
                throw new IllegalArgumentException("Specified string is too long to be synchronized.");
            }
            this.value = value;
            setDirty();
        }
    }

    // --------------------------------------------------------------------- //
    // SynchronizedValue

    public void serialize(final PacketBuffer packet, @Nullable final List<Object> tokens) {
        packet.writeString(value);
    }

    public void deserialize(final PacketBuffer packet) {
        value = packet.readStringFromBuffer(MAX_LENGTH);
    }
}
