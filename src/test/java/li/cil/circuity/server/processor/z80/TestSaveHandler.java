package li.cil.circuity.server.processor.z80;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

import java.io.File;

final class TestSaveHandler implements ISaveHandler {
    private final WorldInfo info;

    TestSaveHandler(final WorldInfo info) {
        this.info = info;
    }

    @Override
    public WorldInfo loadWorldInfo() {
        return info;
    }

    @Override
    public void checkSessionLock() throws MinecraftException {
    }

    @Override
    public IChunkLoader getChunkLoader(final WorldProvider provider) {
        return null;
    }

    @Override
    public void saveWorldInfoWithPlayer(final WorldInfo worldInformation, final NBTTagCompound tagCompound) {
    }

    @Override
    public void saveWorldInfo(final WorldInfo worldInformation) {
    }

    @Override
    public IPlayerFileData getPlayerNBTManager() {
        return null;
    }

    @Override
    public void flush() {
    }

    @Override
    public File getWorldDirectory() {
        return null;
    }

    @Override
    public File getMapFileFromName(final String mapName) {
        return null;
    }

    @Override
    public TemplateManager getStructureTemplateManager() {
        return null;
    }
}
