package li.cil.lib.util;

import com.google.common.base.Throwables;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Predicate;

public final class ReflectionUtil {
    private static final Map<String, Class<?>> PRIMITIVE_CLASSES_BY_NAME = new HashMap<>();
    private static final Map<Class, Constructor> DEFAULT_CONSTRUCTORS = new HashMap<>();
    private static final Map<Class, Map<Class, List<Field>>> ANNOTATED_FIELD_CACHE = new HashMap<>();
    private static final Map<Class, Map<Class, List<Field>>> TYPED_FIELD_CACHE = new HashMap<>();

    static {
        PRIMITIVE_CLASSES_BY_NAME.put("int", int.class);
        PRIMITIVE_CLASSES_BY_NAME.put("boolean", boolean.class);
        PRIMITIVE_CLASSES_BY_NAME.put("float", float.class);
        PRIMITIVE_CLASSES_BY_NAME.put("long", long.class);
        PRIMITIVE_CLASSES_BY_NAME.put("short", short.class);
        PRIMITIVE_CLASSES_BY_NAME.put("byte", byte.class);
        PRIMITIVE_CLASSES_BY_NAME.put("double", double.class);
        PRIMITIVE_CLASSES_BY_NAME.put("char", char.class);
        PRIMITIVE_CLASSES_BY_NAME.put("void", void.class);
    }

    // --------------------------------------------------------------------- //

    public static Class<?> getClass(final String name) throws ClassNotFoundException {
        final Class<?> primitive = PRIMITIVE_CLASSES_BY_NAME.get(name);
        return (primitive != null) ? primitive : Class.forName(name);
    }

    // --------------------------------------------------------------------- //

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(final Class<?> clazz) {
        try {
            final Constructor<?> constructor = getConstructor(clazz);
            return (T) constructor.newInstance();
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new IllegalArgumentException("Failed to invoke constructor.", e);
        }
    }

    public static <T> T newInstance(final Constructor<T> constructor, final Object... initargs) {
        try {
            return constructor.newInstance(initargs);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Failed to invoke constructor.", e);
        }
    }

    public static Constructor<?> getConstructor(final Class<?> clazz) {
        return DEFAULT_CONSTRUCTORS.computeIfAbsent(clazz, ReflectionUtil::computeConstructor);
    }

    public static Constructor<?> getConstructor(final Class<?> clazz, final Class<?>... parameterTypes) {
        try {
            final Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (final NoSuchMethodException e) {
            final StringJoiner joiner = new StringJoiner(", ");
            Arrays.stream(parameterTypes).map(Class::getName).forEach(joiner::add);
            throw new IllegalArgumentException("Type " + clazz.getName() + " has no constructor with the specified parameter types (" + joiner.toString() + ").", e);
        }
    }

    // --------------------------------------------------------------------- //

    public static List<Field> getFieldsWithAnnotation(final Class<?> clazz, final Class<? extends Annotation> annotation) {
        synchronized (ANNOTATED_FIELD_CACHE) {
            return ANNOTATED_FIELD_CACHE.
                    computeIfAbsent(clazz, unused -> new HashMap<>()).
                    computeIfAbsent(annotation, unused -> findFields(clazz, f -> f.isAnnotationPresent(annotation)));
        }
    }

    public static List<Field> getFieldsByType(final Class<?> clazz, final Class<?> fieldType) {
        synchronized (TYPED_FIELD_CACHE) {
            return TYPED_FIELD_CACHE.
                    computeIfAbsent(clazz, unused -> new HashMap<>()).
                    computeIfAbsent(fieldType, unused -> findFields(clazz, f -> fieldType.isAssignableFrom(f.getType())));
        }
    }

    public static Predicate<? super Object> hasAnnotation(final Class<? extends Annotation> annotationClass) {
        return object -> object != null && object.getClass().isAnnotationPresent(annotationClass);
    }

    // --------------------------------------------------------------------- //

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T get(final Object object, final Field field) {
        try {
            return (T) field.get(object);
        } catch (final IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }

    public static void set(final Object object, final Field field, @Nullable final Object value) {
        try {
            field.set(object, value);
        } catch (final IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }

    // --------------------------------------------------------------------- //

    private static Constructor<?> computeConstructor(final Class<?> clazz) {
        try {
            final Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor;
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException("Type " + clazz.getName() + " has no default constructor.", e);
        }
    }

    private static List<Field> findFields(final Class<?> clazz, final Predicate<Field> filter) {
        final List<Field> fields = new ArrayList<>();
        collectFields(clazz, filter, fields);
        fields.forEach(field -> field.setAccessible(true));
        return fields;
    }

    private static void collectFields(@Nullable final Class<?> clazz, final Predicate<Field> filter, final List<Field> output) {
        if (clazz == null) return;
        Arrays.stream(clazz.getDeclaredFields()).filter(filter).forEach(output::add);
        collectFields(clazz.getSuperclass(), filter, output);
    }

    private ReflectionUtil() {
    }
}
