package li.cil.lib.api.synchronization;

/**
 * Implement this interface on a component with {@link SynchronizedValue} fields to
 * receive a notification when the fields change.
 * <p>
 * If a component implements this interface, whenever the client receives new
 * data for any number of synchronized fields, {@link #onSynchronize(Iterable)}
 * will be called with the list of synchronized values that were updated.
 */
public interface SynchronizationListener {
    /**
     * Called with a list of changed synchronized values whenever they change.
     *
     * @param values the list of synchronized values that changed.
     */
    void onSynchronize(final Iterable<SynchronizedValue> values);
}
