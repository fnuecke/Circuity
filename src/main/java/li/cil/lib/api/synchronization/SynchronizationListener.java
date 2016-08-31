package li.cil.lib.api.synchronization;

/**
 * Implement this interface on a component with {@link SynchronizedValue} fields to
 * receive a notification when the fields change.
 * <p>
 * If a component implements this interface, whenever the client receives new
 * data for any number of synchronized fields, {@link #onBeforeSynchronize(Iterable)}
 * and {@link #onAfterSynchronize(Iterable)} will be called with the list of
 * synchronized values that will be/were updated (<code>onBeforeSynchronize</code>
 * is called before the new values are applied, <code>onAfterSynchronize</code>
 * is called after they have been applied).
 */
public interface SynchronizationListener {
    /**
     * Called with a list of synchronized values that will be updated.
     *
     * @param values the list of synchronized values that will change.
     */
    void onBeforeSynchronize(final Iterable<SynchronizedValue> values);

    /**
     * Called with a list of changed synchronized values after they were updated.
     *
     * @param values the list of synchronized values that changed.
     */
    void onAfterSynchronize(final Iterable<SynchronizedValue> values);
}
