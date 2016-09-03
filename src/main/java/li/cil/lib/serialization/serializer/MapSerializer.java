package li.cil.lib.serialization.serializer;

import li.cil.lib.ModSillyBee;
import li.cil.lib.serialization.SerializationException;
import li.cil.lib.serialization.SerializerCollectionImpl;
import li.cil.lib.util.ReflectionUtil;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapSerializer implements Serializer {
    private static final String KEY_CLASS_TAG = "keyClass";
    private static final String KEY_TAG = "key";
    private static final String VALUE_CLASS_TAG = "keyClass";
    private static final String VALUE_TAG = "value";
    private static final String ITEM_CLASSES_TAG = "classes";
    private static final String ENTRIES_TAG = "entries";

    // --------------------------------------------------------------------- //

    private final SerializerCollectionImpl serialization;

    // --------------------------------------------------------------------- //

    public MapSerializer(final SerializerCollectionImpl serialization) {
        this.serialization = serialization;
    }

    // --------------------------------------------------------------------- //
    // Serializer

    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return Map.class.isAssignableFrom(clazz);
    }

    @Override
    public NBTBase serialize(final Object object) {
        final NBTTagList mapTag = new NBTTagList();
        final Map<Object, Object> map = (Map) object;

        // List of types in the map, used as lookup table to avoid
        // having to write the full type name for each entry.
        final List<Class<?>> entryClasses = collectClasses(map);

        for (final Map.Entry entry : map.entrySet()) {
            final NBTTagCompound itemInfoTag = new NBTTagCompound();
            itemInfoTag.setInteger(KEY_CLASS_TAG, entry.getKey() == null ? -1 : entryClasses.indexOf(entry.getKey().getClass()));
            itemInfoTag.setInteger(VALUE_CLASS_TAG, entry.getValue() == null ? -1 : entryClasses.indexOf(entry.getValue().getClass()));
            if (entry.getKey() != null) itemInfoTag.setTag(KEY_TAG, serialization.serialize(entry.getKey()));
            if (entry.getValue() != null) itemInfoTag.setTag(VALUE_TAG, serialization.serialize(entry.getValue()));
            mapTag.appendTag(itemInfoTag);
        }

        final NBTTagCompound tag = new NBTTagCompound();
        if (entryClasses.size() > 1) {
            tag.setTag(ITEM_CLASSES_TAG, serialization.serialize(entryClasses));
        } else if (entryClasses.size() == 1) {
            // Minor compression; also allows calling this recursively on the itemClasses.
            tag.setTag(ITEM_CLASSES_TAG, serialization.serialize(entryClasses.get(0)));
        } // else: map is empty/all entries are null.
        tag.setTag(ENTRIES_TAG, mapTag);
        return tag;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tagBase) {
        final Map<Object, Object> oldInstance = (Map) object;
        final Map<Object, Object> instance = new HashMap();
        final NBTTagCompound wrapper = (NBTTagCompound) tagBase;
        final List<Class<?>> entryClasses;

        if (wrapper.hasKey(ITEM_CLASSES_TAG)) {
            final NBTBase itemClassesTag = wrapper.getTag(ITEM_CLASSES_TAG);

            // Rebuild lookup table, taking into account possible compression (one type).
            if (itemClassesTag instanceof NBTTagCompound) {
                entryClasses = serialization.deserialize(ArrayList.class, itemClassesTag);
            } else {
                entryClasses = new ArrayList<>();
                entryClasses.add(serialization.deserialize(Class.class, itemClassesTag));
            }
        } else {
            entryClasses = Collections.emptyList(); // All null.
        }
        final NBTTagList mapTag = (NBTTagList) wrapper.getTag(ENTRIES_TAG);

        // Try re-using previously present entries with the same keys in the map.
        for (int newIndex = 0; newIndex < mapTag.tagCount(); newIndex++) {
            final NBTTagCompound itemInfoTag = mapTag.getCompoundTagAt(newIndex);
            final int keyClassIndex = itemInfoTag.getInteger(KEY_CLASS_TAG);
            final int valueClassIndex = itemInfoTag.getInteger(VALUE_CLASS_TAG);

            final Object key, value;
            if (keyClassIndex < 0) {
                key = null;
            } else {
                final Class<?> keyClass = entryClasses != null ? entryClasses.get(keyClassIndex) : null;
                if (keyClass == null) {
                    ModSillyBee.getLogger().error("Failed deserializing map entry key, class is null.");
                    continue;
                }

                final NBTBase keyTag = itemInfoTag.getTag(KEY_TAG);
                try {
                    key = serialization.deserialize(keyClass, keyTag);
                } catch (final SerializationException e) {
                    ModSillyBee.getLogger().error("Failed deserializing map entry key.", e);
                    continue;
                }
            }

            final Object oldValue = (oldInstance != null) ? oldInstance.get(key) : null;
            if (valueClassIndex < 0) {
                value = null;
            } else {
                final Class<?> valueClass = entryClasses != null ? entryClasses.get(valueClassIndex) : null;
                if (valueClass == null) {
                    ModSillyBee.getLogger().error("Failed deserializing map entry value, class is null.");
                    continue;
                }

                final NBTBase valueTag = itemInfoTag.getTag(VALUE_TAG);
                try {
                    if (oldValue != null && oldValue.getClass() == valueClass) {
                        value = serialization.deserialize(oldValue, valueClass, valueTag);
                    } else {
                        value = serialization.deserialize(valueClass, valueTag);
                    }
                } catch (final SerializationException e) {
                    ModSillyBee.getLogger().error("Failed deserializing map entry value.", e);
                    continue;
                }
            }

            instance.put(key, value);
        }

        return ReflectionUtil.newInstance(ReflectionUtil.getConstructor(clazz, Map.class), instance);
    }

    @Nullable
    @Override
    public Object getDefault() {
        return null;
    }

    // --------------------------------------------------------------------- //

    private static List<Class<?>> collectClasses(final Map<Object, Object> map) {
        final List<Class<?>> classes = new ArrayList<>();
        for (final Map.Entry entry : map.entrySet()) {
            collectClasses(entry.getKey(), classes);
            collectClasses(entry.getValue(), classes);
        }
        return classes;
    }

    private static void collectClasses(@Nullable final Object object, final List<Class<?>> classes) {
        if (object == null) {
            return;
        }
        final Class<?> clazz = object.getClass();
        if (!classes.contains(clazz)) {
            classes.add(clazz);
        }
    }
}
