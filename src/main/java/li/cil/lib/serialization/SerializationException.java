package li.cil.lib.serialization;

public final class SerializationException extends RuntimeException {
    public SerializationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SerializationException(final String message) {
        super(message);
    }
}
