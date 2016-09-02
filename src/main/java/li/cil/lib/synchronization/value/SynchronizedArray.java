package li.cil.lib.synchronization.value;

import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.api.synchronization.SynchronizationManagerServer;
import li.cil.lib.api.synchronization.SynchronizedValue;
import li.cil.lib.util.NBTUtil;
import net.minecraft.nbt.NBTBase;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Serializable
public final class SynchronizedArray<E> extends AbstractList<E> implements SynchronizedValue {
    private final Object lock = new Object(); // To avoid changes to array during serialization.
    private final Class<E> valueClass;

    @Serialize
    private E[] value;

    private SynchronizationManagerServer manager;

    // --------------------------------------------------------------------- //

    @SuppressWarnings("unchecked")
    public SynchronizedArray(final Class<E> componentType, final int initialCapacity) {
        valueClass = componentType;
        value = (E[]) Array.newInstance(componentType, initialCapacity);
    }

    public SynchronizedArray(final Class<E> componentType) {
        this(componentType, 0);
    }

    public SynchronizedArray(final Class<E> componentType, final E[] value) {
        this.valueClass = componentType;
        this.value = value.clone();
    }

    // --------------------------------------------------------------------- //

    public void setSize(final int size) {
        synchronized (lock) {
            value = Arrays.copyOf(value, size);
            setDirty(-1);
        }
    }

    public void fill(final E item) {
        synchronized (lock) {
            Arrays.fill(value, item);
            setDirty(-1);
        }
    }

    /**
     * Get the raw underlying array.
     * <p>
     * This is exposed <em>purely for reading</em>, for performance sensitive
     * use-cases (e.g. iterating the full array). Again, do <em>not</em>
     * write to this, as changes may lead to synchronization bugs.
     *
     * @return the underlying array.
     */
    public E[] array() {
        return value;
    }

    // --------------------------------------------------------------------- //
    // AbstractList

    @Override
    public int size() {
        return value.length;
    }

    @Override
    public E get(final int index) {
        return value[index];
    }

    @Override
    public E set(final int index, final E element) {
        if (ObjectUtils.notEqual(value[index], element)) {
            synchronized (lock) {
                value[index] = element;
                setDirty(index);
            }
        }
        return element;
    }

    @Override
    public boolean equals(final Object o) {
        return o == this;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    public void setManager(@Nullable final SynchronizationManagerServer manager) {
        this.manager = manager;
    }

    // --------------------------------------------------------------------- //
    // SynchronizedValue

    @Override
    public void serialize(final PacketBuffer packet, @Nullable final List<Object> tokens) {
        synchronized (lock) {
            if (tokens == null || tokens.contains(-1)) {
                packet.writeBoolean(true);
                serializeComplete(packet);
            } else {
                packet.writeBoolean(false);
                serializePartial(packet, new LinkedHashSet<>(tokens));
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void deserialize(final PacketBuffer packet) {
        if (packet.readBoolean()) {
            deserializeComplete(packet);
        } else {
            deserializePartial(packet);
        }
    }

    // --------------------------------------------------------------------- //

    private void setDirty(final int index) {
        if (manager != null) {
            manager.setDirty(this, index);
        }
    }

    private void serializeComplete(final PacketBuffer packet) {
        packet.writeVarIntToBuffer(value.length);

        final Stream<E> elements = Arrays.stream(value);
        final Class commonType = findAndWriteCommonElementType(elements, packet);

        for (final E element : value) {
            writeElement(element, packet, commonType);
        }
    }

    private void serializePartial(final PacketBuffer packet, final Set<Object> indices) {
        packet.writeVarIntToBuffer(indices.size());

        final Stream<E> elements = indices.stream().map(o -> value[(Integer) o]);
        final Class commonType = findAndWriteCommonElementType(elements, packet);

        for (final Object i : indices) {
            final int index = (Integer) i;
            packet.writeVarIntToBuffer(index);
            writeElement(value[index], packet, commonType);
        }
    }

    private void deserializeComplete(final PacketBuffer packet) {
        final int count = packet.readVarIntFromBuffer();
        value = Arrays.copyOf(value, count);

        final Class commonType = tryReadCommonElementType(packet);

        for (int index = 0; index < count; index++) {
            final E element = readElement(packet, commonType);
            value[index] = element;
        }
    }

    private void deserializePartial(final PacketBuffer packet) {
        final int count = packet.readVarIntFromBuffer();

        final Class commonType = tryReadCommonElementType(packet);

        for (int i = 0; i < count; i++) {
            final int index = packet.readVarIntFromBuffer();
            final E element = readElement(packet, commonType);
            value[index] = element;
        }
    }

    @Nullable
    private Class findAndWriteCommonElementType(final Stream<E> elements, final PacketBuffer packet) {
        // Size optimization: if element type is final, all elements must have
        // the same type, and we don't even need to write and read it.
        if (Modifier.isFinal(valueClass.getModifiers())) {
            return valueClass;
        }

        final SynchronizationManagerServer synchronization = SillyBeeAPI.synchronization.getServer();
        boolean areAllElementsOfSameType = true;
        Class commonType = null;
        final Iterator<E> elementIterator = elements.iterator();
        while (elementIterator.hasNext()) {
            final E element = elementIterator.next();
            final Class elementType = (element == null) ? null : element.getClass();
            if (commonType == null) {
                commonType = elementType;
            } else if (elementType != commonType) {
                areAllElementsOfSameType = false;
                break;
            }
        }

        packet.writeBoolean(areAllElementsOfSameType);
        if (areAllElementsOfSameType) {
            packet.writeVarIntToBuffer(synchronization.getTypeIdByType(commonType));
        }

        return areAllElementsOfSameType ? commonType : NoCommonType.class;
    }

    private void writeElement(@Nullable final E element, final PacketBuffer packet, @Nullable final Class commonType) {
        if (commonType == NoCommonType.class) {
            packet.writeVarIntToBuffer(SillyBeeAPI.synchronization.getServer().getTypeIdByValue(element));
            if (element == null) {
                return; // Type being null implies value is null when reading.
            }
        }
        if (commonType != null) {
            NBTUtil.write(element != null ? SillyBeeAPI.serialization.get(Side.SERVER).serialize(element) : null, packet);
        }
    }

    @Nullable
    private Class tryReadCommonElementType(final PacketBuffer packet) {
        // Size optimization: if element type is final, all elements must have
        // the same type, and we don't even need to write and read it.
        if (Modifier.isFinal(valueClass.getModifiers())) {
            return valueClass;
        }

        final boolean areAllElementsOfSameType = packet.readBoolean();
        final Class commonType;
        if (areAllElementsOfSameType) {
            commonType = SillyBeeAPI.synchronization.getClient().getTypeByTypeId(packet.readVarIntFromBuffer());
        } else {
            commonType = NoCommonType.class;
        }
        return commonType;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private E readElement(final PacketBuffer packet, @Nullable final Class commonType) {
        final Class type;
        if (commonType != NoCommonType.class) {
            type = commonType;
        } else {
            type = SillyBeeAPI.synchronization.getClient().getTypeByTypeId(packet.readVarIntFromBuffer());
        }
        if (type == null) {
            return null;
        }
        final NBTBase elementNbt = NBTUtil.read(packet);
        if (elementNbt == null) {
            return null;
        }
        return (E) SillyBeeAPI.serialization.get(Side.CLIENT).deserialize(type, elementNbt);
    }

    private final class NoCommonType {
    }
}
