package li.cil.lib;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.StorageAPI;
import li.cil.lib.api.event.ForwardedFMLServerStoppedEvent;
import li.cil.lib.api.storage.ExternalData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.UUID;

public enum Storage implements StorageAPI {
    INSTANCE;

    // --------------------------------------------------------------------- //

    /**
     * Map data ID to all loaded data wrappers.
     * <p>
     * Those will never ge unloaded until the server is shut down, but they
     * lazily load their actual data, so the memory overhead is minimal.
     */
    private final HashMap<UUID, ExternalDataImpl> loadedData = new HashMap<>();

    // --------------------------------------------------------------------- //

    public static void init() {
        SillyBeeAPI.storage = INSTANCE;
        SillyBeeAPI.EVENT_BUS.register(INSTANCE);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void handleWorldSave(final WorldEvent.Save event) {
        loadedData.values().removeIf(ExternalDataImpl::flush);
    }

    @SubscribeEvent
    public void handleServerStopped(final ForwardedFMLServerStoppedEvent event) {
        loadedData.clear();
    }

    // --------------------------------------------------------------------- //

    @Override
    public ExternalData getData(@Nullable final UUID id) {
        final UUID idToLoad = id == null ? UUID.randomUUID() : id;
        return loadedData.computeIfAbsent(idToLoad, ExternalDataImpl::new);
    }

    // --------------------------------------------------------------------- //

    private static File getDataFile(final UUID uuid, final boolean createDirectories) {
        final String uuidString = uuid.toString();
        final File basePath = new File(DimensionManager.getCurrentSaveRootDirectory(), SillyBeeAPI.MOD_ID);
        final File groupPath = new File(basePath, uuidString.substring(0, 1));
        if (createDirectories) groupPath.mkdirs();
        return new File(groupPath, uuidString + ".bin");
    }

    private static void copyFileToBuffer(final File filePath, final ByteBuf buffer) {
        buffer.resetReaderIndex();
        buffer.resetWriterIndex();

        try (final FileInputStream stream = new FileInputStream(filePath)) {
            final BufferedInputStream buffered = new BufferedInputStream(stream);
            int read;
            while ((read = buffered.read()) != -1) {
                buffer.writeByte(read);
            }
        } catch (final IOException e) {
            ModSillyBee.getLogger().warn("Failed loading externally stored data.", e);
        }
    }

    private static void copyBufferToFile(final File filePath, final ByteBuf buffer) {
        try (final FileOutputStream stream = new FileOutputStream(filePath)) {
            final BufferedOutputStream buffered = new BufferedOutputStream(stream);
            buffered.write(buffer.array());
        } catch (final IOException e) {
            ModSillyBee.getLogger().warn("Failed saving externally stored data.", e);
        }
    }

    // --------------------------------------------------------------------- //

    private final class ExternalDataImpl implements ExternalData {
        /**
         * The ID of the data we're wrapping.
         */
        private final UUID uuid;

        /**
         * A weak reference to the buffer holding our current data.
         * <p>
         * When <code>null</code>, implies the data should be removed from disk.
         */
        private WeakReference<ByteBuf> dataRef;

        /**
         * A hard reference to the buffer holding our current data.
         * <p>
         * When <em>not</em> <code>null</code>, implies the data has changed and
         * should be flushed to disk. This also ensures we don't lose said
         * changes due to the weak reference.
         */
        private ByteBuf data;

        /**
         * The timestamp of the data when it was last read.
         * <p>
         * Used to detect external changes to reload the data if it hasn't
         * changed in memory but on disk (external changes made by the user).
         */
        private long fileTime;

        // --------------------------------------------------------------------- //

        public ExternalDataImpl(final UUID uuid) {
            this.uuid = uuid;
            this.dataRef = new WeakReference<>(null);
        }

        // --------------------------------------------------------------------- //

        /**
         * Flushes any changes made to the data to disk.
         *
         * @return <code>true</code> if the data was removed; <code>false</code> otherwise.
         */
        public boolean flush() {
            final File filePath = getDataFile(uuid, true);

            final boolean isDeleted = dataRef == null;
            if (isDeleted) {
                filePath.delete();
                return true;
            }

            final boolean hasChanged = data != null;
            if (hasChanged) {
                copyBufferToFile(filePath, data);
                data = null;
                return false;
            }

            final ByteBuf loadedData = dataRef.get();
            if (loadedData != null) {
                final boolean hasSourceChanged = filePath.exists() && filePath.lastModified() > fileTime;
                if (hasSourceChanged) {
                    copyFileToBuffer(filePath, loadedData);
                    fileTime = filePath.lastModified();
                }
            }

            return false;
        }

        // --------------------------------------------------------------------- //

        @Override
        public UUID getDataId() {
            return uuid;
        }

        @Override
        public ByteBuf getData() {
            if (dataRef == null) {
                throw new IllegalStateException("Trying to access deleted data.");
            }

            // If we have a hard-ref, just use that as a shortcut.
            if (data != null) {
                return data;
            }

            // Otherwise check if our weak-ref is pointing to something.
            final ByteBuf loadedData = dataRef.get();
            if (loadedData != null) {
                return loadedData;
            }

            // Otherwise we need to load the data.
            final File filePath = getDataFile(uuid, false);
            final ByteBuf buffer = Unpooled.buffer();
            dataRef = new WeakReference<>(buffer);

            if (filePath.exists()) {
                copyFileToBuffer(filePath, buffer);
                fileTime = filePath.lastModified();
            } else {
                // Newly created, flag self as dirty so it gets persisted.
                data = buffer;
                fileTime = 0;
            }

            return buffer;
        }

        @Override
        public void markChanged() {
            data = getData();
        }

        @Override
        public void markDeleted() {
            data = null;
            dataRef = null;
        }
    }
}
