package li.cil.lib.api.storage;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

/**
 * Represents a reference to some externally stored data.
 * <p>
 * The {@link ByteBuf} held by this reference is automatically saved when a
 * world is saved, and persisted onto disk in a location separate to region
 * or player information. This is primarily intended to be used for data that
 * may be separately be edited by the user, or for large data blobs that should
 * not be saved in the region files.
 */
public interface ExternalData {
    /**
     * The ID uniquely identifying this piece of data.
     * <p>
     * May be useful to store an indirect reference to the data, so that it can
     * be retrieved again later using {@link li.cil.lib.api.StorageAPI#getData(UUID)}.
     *
     * @return the ID of this data.
     */
    UUID getDataId();

    /**
     * The actual byte buffer holding this data.
     * <p>
     * This will always return the same object after having been loaded once.
     * As such you may store a direct reference to the byte buffer. In fact, it
     * is strongly encouraged to do so, as failing to do so may lead to the
     * underlying data getting garbage collected and reloaded in the next call,
     * which will typically pretty bad performance-wise.
     * <p>
     * Modify this buffer as you see fit, but make sure to call {@link #markChanged()}
     * if you wish the changes to persist, as they will not be flushed to disk
     * otherwise.
     *
     * @return the underlying data buffer holding the actual data.
     */
    ByteBuf getData();

    /**
     * Call this after making changes to the {@link ByteBuf} returned from
     * {@link #getData()} to make the changes persist. Unless this is called,
     * changes will be lost when the server is shut down.
     */
    void markChanged();

    /**
     * Call this to schedule the data for removal from disk upon the next save
     * operation. The data should no longer be accessed after this has been
     * called, and all references to it should be dropped.
     */
    void markDeleted();
}
