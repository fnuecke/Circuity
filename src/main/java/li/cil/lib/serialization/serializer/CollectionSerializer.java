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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CollectionSerializer implements Serializer {
    private static final String ITEM_CLASS_TAG = "class";
    private static final String ITEM_TAG = "value";
    private static final String ITEM_CLASSES_TAG = "classes";
    private static final String ITEMS_TAG = "values";

    // --------------------------------------------------------------------- //

    private final SerializerCollectionImpl serialization;

    // --------------------------------------------------------------------- //

    public CollectionSerializer(final SerializerCollectionImpl serialization) {
        this.serialization = serialization;
    }

    // --------------------------------------------------------------------- //
    // Serializer

    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz);
    }

    @Override
    public NBTBase serialize(final Object object) {
        final NBTTagList collectionTag = new NBTTagList();
        final Collection collection = (Collection) object;

        // List of types in the collection, used as lookup table to avoid
        // having to write the full type name for each entry.
        final List<Class<?>> itemClasses = collectClasses(collection);

        for (final Object item : collection) {
            final NBTTagCompound itemInfoTag = new NBTTagCompound();
            itemInfoTag.setInteger(ITEM_CLASS_TAG, item == null ? -1 : itemClasses.indexOf(item.getClass()));
            if (item != null) itemInfoTag.setTag(ITEM_TAG, serialization.serialize(item));
            collectionTag.appendTag(itemInfoTag);
        }

        final NBTTagCompound tag = new NBTTagCompound();
        if (itemClasses.size() > 1) {
            tag.setTag(ITEM_CLASSES_TAG, serialization.serialize(itemClasses));
        } else if (itemClasses.size() == 1) {
            // Minor compression; also allows calling this recursively on the itemClasses.
            tag.setTag(ITEM_CLASSES_TAG, serialization.serialize(itemClasses.get(0)));
        } // else: all items are null.
        tag.setTag(ITEMS_TAG, collectionTag);
        return tag;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tagBase) {
        final Collection oldInstance = (Collection) object;
        final Collection instance = new ArrayList();
        final NBTTagCompound wrapper = (NBTTagCompound) tagBase;
        final List<Class<?>> itemClasses;

        if (wrapper.hasKey(ITEM_CLASSES_TAG)) {
            final NBTBase itemClassesTag = wrapper.getTag(ITEM_CLASSES_TAG);

            // Rebuild lookup table, taking into account possible compression (one type).
            if (itemClassesTag instanceof NBTTagCompound) {
                itemClasses = serialization.deserialize(ArrayList.class, itemClassesTag);
            } else {
                itemClasses = new ArrayList<>();
                itemClasses.add(serialization.deserialize(Class.class, itemClassesTag));
            }
        } else {
            itemClasses = Collections.emptyList(); // All null.
        }
        final NBTTagList collectionTag = (NBTTagList) wrapper.getTag(ITEMS_TAG);

        // Try re-using previously present items at the same position in the collection.
        final Iterator iterator = oldInstance != null ? oldInstance.iterator() : null;
        for (int newIndex = 0; newIndex < collectionTag.tagCount(); newIndex++) {
            final NBTTagCompound itemInfoTag = collectionTag.getCompoundTagAt(newIndex);
            final int itemClassIndex = itemInfoTag.getInteger(ITEM_CLASS_TAG);

            final Object target;
            if (iterator != null && iterator.hasNext()) {
                target = iterator.next();
            } else {
                target = null;
            }

            if (itemClassIndex < 0) {
                instance.add(null);
            } else {
                final Class<?> itemClass = itemClasses != null ? itemClasses.get(itemClassIndex) : null;
                final NBTBase itemTag = itemInfoTag.getTag(ITEM_TAG);

                if (itemClass != null) {
                    try {
                        if (target != null && target.getClass() == itemClass) {
                            instance.add(serialization.deserialize(target, itemClass, itemTag));
                        } else {
                            instance.add(serialization.deserialize(itemClass, itemTag));
                        }
                    } catch (final SerializationException e) {
                        ModSillyBee.getLogger().error("Failed deserializing collection item.", e);
                        instance.add(null);
                    }
                } else {
                    ModSillyBee.getLogger().error("Failed deserializing collection item, class is null.");
                    instance.add(null);
                }
            }
        }

        return ReflectionUtil.newInstance(ReflectionUtil.getConstructor(clazz, Collection.class), instance);
    }

    @Nullable
    @Override
    public Object getDefault() {
        return null;
    }

    // --------------------------------------------------------------------- //

    private static List<Class<?>> collectClasses(final Collection collection) {
        final List<Class<?>> classes = new ArrayList<>();
        for (final Object item : collection) {
            if (item == null) continue;
            final Class<?> clazz = item.getClass();
            if (!classes.contains(clazz)) {
                classes.add(clazz);
            }
        }
        return classes;
    }
}
