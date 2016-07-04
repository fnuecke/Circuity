package li.cil.lib.synchronization.value;

import li.cil.lib.api.synchronization.SynchronizationManagerServer;
import li.cil.lib.api.synchronization.SynchronizedValue;

import javax.annotation.Nullable;

/**
 * Utility base class for simple synchronized values.
 */
public abstract class AbstractSynchronizedValue implements SynchronizedValue {
    protected SynchronizationManagerServer manager;

    // --------------------------------------------------------------------- //

    protected void setDirty() {
        if (manager != null) {
            manager.setDirty(this, null);
        }
    }

    // --------------------------------------------------------------------- //
    // SynchronizedValue

    public void setManager(@Nullable final SynchronizationManagerServer manager) {
        this.manager = manager;
    }
}
