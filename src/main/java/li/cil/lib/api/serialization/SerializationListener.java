package li.cil.lib.api.serialization;

/**
 * Implementing this on an object with the {@link Serializable} annotation
 * will make the serializer call the methods in this interface at their
 * respective times during serialization / deserialization.
 * <p>
 * This provides a large amount of flexibility, for example for converting
 * some complex runtime structure to a more simple format, which is then
 * serialized instead. It also allows adjusting state, e.g. references to
 * a value that may be replaced during deserialization.
 */
public interface SerializationListener {
    /**
     * Called before the value is serialized.
     */
    void onBeforeSerialization();

    /**
     * Called after the value has been serialized.
     */
    void onAfterSerialization();

    /**
     * Called before the value will be deserialized.
     */
    void onBeforeDeserialization();

    /**
     * Called after the value has been deserialized.
     */
    void onAfterDeserialization();
}
