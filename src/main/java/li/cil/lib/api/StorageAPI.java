package li.cil.lib.api;

import li.cil.lib.api.storage.ExternalData;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Access to external data management facilities.
 */
public interface StorageAPI {
    /**
     * Get a data reference to the data with the specified ID.
     * <p>
     * When passing <code>null</code>, a new data object will be allocated and
     * returned.
     *
     * @param id the ID of the data to retrieve.
     * @return a wrapper for the loaded data.
     */
    ExternalData getData(@Nullable final UUID id);
}
