package li.cil.lib.synchronization.value;

import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.util.List;

@Serializable
public final class SynchronizedEnum<T extends Enum<T>> extends AbstractSynchronizedValue {
    private final T[] enumConstants;

    @Serialize
    private int value;

    // --------------------------------------------------------------------- //

    public SynchronizedEnum(final Class<T> enumClass) {
        enumConstants = enumClass.getEnumConstants();
    }

    public SynchronizedEnum(final Class<T> enumClass, final T value) {
        this(enumClass);
        this.value = value.ordinal();
    }

    // --------------------------------------------------------------------- //

    public T get() {
        return enumConstants[value];
    }

    public void set(final T value) {
        final int ordinal = value.ordinal();
        if (this.value != ordinal) {
            this.value = ordinal;
            setDirty();
        }
    }

    // --------------------------------------------------------------------- //
    // SynchronizedValue

    public void serialize(final PacketBuffer packet, @Nullable final List<Object> tokens) {
        packet.writeVarIntToBuffer(value);
    }

    public void deserialize(final PacketBuffer packet) {
        value = packet.readVarIntFromBuffer();
    }
}
