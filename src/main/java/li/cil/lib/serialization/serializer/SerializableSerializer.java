package li.cil.lib.serialization.serializer;

import li.cil.lib.ModSillyBee;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.api.serialization.Serialize;
import li.cil.lib.serialization.SerializationException;
import li.cil.lib.serialization.SerializerCollectionImpl;
import li.cil.lib.util.ReflectionUtil;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;

public final class SerializableSerializer implements Serializer {
    private static final String FIELD_CLASS_TAG = "fieldClass";
    private static final String FIELD_TAG = "field";

    // --------------------------------------------------------------------- //

    private final SerializerCollectionImpl serialization;

    // --------------------------------------------------------------------- //

    public SerializableSerializer(final SerializerCollectionImpl serialization) {
        this.serialization = serialization;
    }

    // --------------------------------------------------------------------- //
    // Serializer

    @Override
    public boolean canSerialize(final Class<?> clazz) {
        return clazz.isAnnotationPresent(Serializable.class);
    }

    @Override
    public NBTBase serialize(final Object object) {
        final NBTTagCompound tag = new NBTTagCompound();
        final List<Field> fields = getFields(object.getClass());

        for (final Field field : fields) {
            try {
                final Object fieldValue = field.get(object);
                final NBTTagCompound fieldInfoTag = new NBTTagCompound();
                if (ObjectUtils.notEqual(fieldValue, serialization.getDefault(field.getType()))) {
                    if (fieldValue != null) {
                        // If the value's type differs from the field type we have to remember that type.
                        if (!areTypesEquivalent(field.getType(), fieldValue.getClass())) {
                            fieldInfoTag.setTag(FIELD_CLASS_TAG, serialization.serialize(fieldValue.getClass()));
                        }
                        fieldInfoTag.setTag(FIELD_TAG, serialization.serialize(fieldValue));
                    }
                }
                tag.setTag(getPersistedName(field), fieldInfoTag);
            } catch (final IllegalAccessException e) {
                throw new SerializationException("Field is not accessible.", e);
            }
        }

        return tag;
    }

    @Nullable
    @Override
    public Object deserialize(@Nullable final Object object, final Class<?> clazz, final NBTBase tagBase) {
        final Object instance = (object != null ? object : ReflectionUtil.newInstance(clazz));

        final NBTTagCompound tag = (NBTTagCompound) tagBase;
        final List<Field> fields = getFields(clazz);

        for (final Field field : fields) {
            try {
                final String name = getPersistedName(field);

                final Object value;
                if (tag.hasKey(name)) {
                    final NBTTagCompound fieldInfoTag = (NBTTagCompound) tag.getTag(name);

                    // Get type of the value, if we explicitly wrote it it's different than the field type.
                    final Class<?> fieldClass;
                    if (fieldInfoTag.hasKey(FIELD_CLASS_TAG)) {
                        fieldClass = serialization.deserialize(Class.class, fieldInfoTag.getTag(FIELD_CLASS_TAG));
                    } else {
                        fieldClass = field.getType();
                    }

                    if (fieldClass != null && fieldInfoTag.hasKey(FIELD_TAG)) {
                        // Get old object to deserialize into. Make sure it's of compatible type.
                        final Object target = field.get(instance);
                        if (target != null && target.getClass() == fieldClass) {
                            value = serialization.deserialize(target, fieldClass, fieldInfoTag.getTag(FIELD_TAG));
                        } else {
                            value = serialization.deserialize(fieldClass, fieldInfoTag.getTag(FIELD_TAG));
                        }
                    } else {
                        if (fieldClass == null) {
                            // If the type is no longer known (can only be null from failed deserialization
                            // of a class type), we have an object type not a primitive, so it's safe to
                            // assign null (the default). Still, warn about it.
                            ModSillyBee.getLogger().warn("Failed deserializing class from " + fieldInfoTag.toString() + ". Class was removed or moved without a remapping being provided.");
                        } // else: didn't serialize type because value was default value.
                        value = serialization.getDefault(field.getType());
                    }

                    if (Modifier.isFinal(field.getModifiers()) && !Objects.equals(field.get(instance), value)) {
                        ModSillyBee.getLogger().warn("Serialized field '" + clazz.getName() + "." + field.getName() + "' is final but will be assigned a new value.");
                    }

                    field.set(instance, value);
                }
            } catch (final IllegalAccessException e) {
                throw new SerializationException("Field is not accessible.", e);
            }
        }

        return instance;
    }

    @Nullable
    @Override
    public Object getDefault() {
        return null;
    }

    // --------------------------------------------------------------------- //

    private static List<Field> getFields(final Class<?> clazz) {
        return ReflectionUtil.getFieldsWithAnnotation(clazz, Serialize.class);
    }

    private static String getPersistedName(final Field field) {
        final String annotationName = field.getAnnotation(Serialize.class).value();
        final String fieldName = field.getName();
        return "".equals(annotationName) ? fieldName : annotationName;
    }

    private static boolean areTypesEquivalent(final Class<?> a, final Class<?> b) throws IllegalAccessException {
        if (a == b) {
            return true;
        }
        if (!a.isPrimitive() && b.isPrimitive()) {
            return isSamePrimitive(a, b);
        }
        if (a.isPrimitive() && !b.isPrimitive()) {
            return isSamePrimitive(b, a);
        }
        return false;
    }

    private static boolean isSamePrimitive(final Class<?> boxed, final Class<?> primitive) throws IllegalAccessException {
        final Field[] fields = boxed.getFields();
        for (final Field field : fields) {
            if ("TYPE".equals(field.getName())) {
                return field.get(null) == primitive;
            }
        }
        return false;
    }
}
