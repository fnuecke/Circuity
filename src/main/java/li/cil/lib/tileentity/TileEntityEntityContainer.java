package li.cil.lib.tileentity;

import li.cil.circuity.common.capabilities.NoSuchCapabilityException;
import li.cil.lib.ModSillyBee;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.serialization.Serializable;
import li.cil.lib.ecs.component.ChunkNotifyingChangeListener;
import li.cil.lib.ecs.component.LocationTileEntity;
import li.cil.lib.ecs.entity.EntityContainerProxy;
import li.cil.lib.util.ReflectionUtil;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class TileEntityEntityContainer extends TileEntity implements EntityContainerProxy {
    private static final String ENTITY_TAG = "entity";
    private static final String COMPONENTS_TAG = "components";

    // --------------------------------------------------------------------- //

    private NBTBase loadedData;
    private long entity;

    // --------------------------------------------------------------------- //
    // EntityContainer

    @Override
    public EntityComponentManager getManager() {
        return SillyBeeAPI.manager.getManager(getWorld());
    }

    @Override
    public long getEntity() {
        return entity;
    }

    // --------------------------------------------------------------------- //
    // TileEntity

    @Override
    public void readFromNBT(final NBTTagCompound compound) {
        super.readFromNBT(compound);
        loadedData = compound.getTag(COMPONENTS_TAG);
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound compound) {
        final NBTTagCompound tag = super.writeToNBT(compound);
        tag.setTag(COMPONENTS_TAG, SillyBeeAPI.serialization.get(getWorld()).serialize(getSerializableComponents()));
        return tag;
    }

    private Iterable<Component> getSerializableComponents() {
        return StreamSupport.stream(getComponents().spliterator(), false).filter(ReflectionUtil.hasAnnotation(Serializable.class)).collect(Collectors.toList());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(getPos(), getBlockMetadata(), getUpdateTag());
    }

    @Override
    public void onDataPacket(final NetworkManager net, final SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        final NBTTagCompound compound = super.getUpdateTag();
        compound.setLong(ENTITY_TAG, getEntity());
        return compound;
    }

    @Override
    public void handleUpdateTag(final NBTTagCompound tag) {
        SillyBeeAPI.synchronization.getClient().subscribe(getManager(), entity = tag.getLong(ENTITY_TAG));
        getWorld().notifyNeighborsOfStateChange(getPos(), getBlockType());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        initialize();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        dispose();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        dispose();
    }

    @Override
    public boolean hasCapability(final Capability<?> capability, final EnumFacing facing) {
        if (super.hasCapability(capability, facing)) {
            return true;
        }

        for (final Component component : getComponents()) {
            if (component instanceof ICapabilityProvider) {
                final ICapabilityProvider provider = (ICapabilityProvider) component;
                if (provider.hasCapability(capability, facing)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public <T> T getCapability(final Capability<T> capability, final EnumFacing facing) {
        final List<T> instances = new ArrayList<>();

        addCapability(super.getCapability(capability, facing), instances);

        for (final Component component : getComponents()) {
            if (component instanceof ICapabilityProvider) {
                final ICapabilityProvider provider = (ICapabilityProvider) component;
                addCapability(provider.getCapability(capability, facing), instances);
            }
        }

        if (instances.isEmpty()) {
            throw new NoSuchCapabilityException();
        }
        if (instances.size() == 1) {
            return instances.get(0);
        }
        return SillyBeeAPI.capabilities.getWrapper(capability, instances);
    }

    private static <T> void addCapability(@Nullable final T instance, final List<T> instances) {
        if (instance != null) {
            instances.add(instance);
        }
    }

    protected void initialize() {
        if (getWorld().isRemote) {
            return;
        }

        assert (getEntity() == 0L);
        entity = getManager().addEntity();
        addComponents();
        if (loadedData != null) {
            try {
                SillyBeeAPI.serialization.get(getWorld()).deserialize(getSerializableComponents(), ArrayList.class, loadedData);
            } catch (final Throwable e) {
                ModSillyBee.getLogger().error("Failed restoring tile entity component data.", e);
            }
            loadedData = null;
        }
    }

    protected void dispose() {
        assert (getEntity() != 0L);
        getManager().removeEntity(getEntity());
        entity = 0L;
    }

    protected void addComponents() {
        //noinspection ConstantConditions Guaranteed to not be null because LocationTilEntity doesn't destroy self in onCreate.
        addComponent(LocationTileEntity.class).setParent(this);
        addComponent(ChunkNotifyingChangeListener.class);
    }
}
