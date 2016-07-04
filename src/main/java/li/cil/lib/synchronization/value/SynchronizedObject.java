package li.cil.lib.synchronization.value;

import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.util.NBTUtil;
import net.minecraft.nbt.NBTBase;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Supplier;

@Serializable
public final class SynchronizedObject<T> extends AbstractSynchronizedValue {
    private final Class<T> valueClass;

    @Serialize
    private T value;

    // --------------------------------------------------------------------- //

    public SynchronizedObject(final Class<T> valueClass) {
        this.valueClass = valueClass;
    }

    public SynchronizedObject(final Class<T> valueClass, final T value) {
        this.valueClass = valueClass;
        this.value = value;
    }

    // --------------------------------------------------------------------- //

    @Nullable
    public T get() {
        return value;
    }

    public T orElse(final T other) {
        if (value != null) {
            return value;
        } else {
            return other;
        }
    }

    public T orElseGet(final Supplier<T> other) {
        if (value != null) {
            return value;
        } else {
            return other.get();
        }
    }

    public <E extends RuntimeException> T orElseThrow(final Supplier<E> exceptionSupplier) {
        if (value != null) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    public void set(@Nullable final T value) {
        if (ObjectUtils.notEqual(this.value, value)) {
            this.value = value;
            setDirty();
        }
    }

    // --------------------------------------------------------------------- //
    // SynchronizedValue

    public void serialize(final PacketBuffer packet, @Nullable final List<Object> tokens) {
        if (value != null) {
            NBTUtil.write(SillyBeeAPI.serialization.get(Side.SERVER).serialize(value), packet);
            if (!Modifier.isFinal(valueClass.getModifiers())) { // No need if final.
                packet.writeVarIntToBuffer(SillyBeeAPI.synchronization.getServer().getTypeIdByType(value.getClass()));
            }
        } else {
            NBTUtil.write(null, packet);
        }
    }

    @SuppressWarnings("unchecked")
    public void deserialize(final PacketBuffer packet) {
        final NBTBase valueNbt = NBTUtil.read(packet);
        if (valueNbt != null) {
            final Class clazz;
            if (Modifier.isFinal(valueClass.getModifiers())) {
                clazz = valueClass;
            } else {
                clazz = SillyBeeAPI.synchronization.getClient().getTypeByTypeId(packet.readVarIntFromBuffer());
            }
            assert (clazz != null);
            value = (T) SillyBeeAPI.serialization.get(Side.CLIENT).deserialize(value, clazz, valueNbt);
        } else {
            value = null;
        }
    }
}
