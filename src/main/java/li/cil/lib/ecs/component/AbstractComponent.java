package li.cil.lib.ecs.component;

import io.netty.buffer.ByteBuf;
import li.cil.lib.ModSillyBee;
import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.api.ecs.component.Location;
import li.cil.lib.api.ecs.component.MessageReceiver;
import li.cil.lib.api.ecs.component.event.ChangeListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.network.Network;
import li.cil.lib.network.message.MessageComponentData;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.Optional;

public abstract class AbstractComponent implements Component, MessageReceiver {
    private final EntityComponentManager manager;
    private final long entity;
    private final long id;

    // --------------------------------------------------------------------- //

    protected AbstractComponent(final EntityComponentManager manager, final long entity, final long id) {
        this.manager = manager;
        this.id = id;
        this.entity = entity;
    }

    // --------------------------------------------------------------------- //
    // Component

    @Override
    public EntityComponentManager getManager() {
        return manager;
    }

    @Override
    public long getEntity() {
        return entity;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDestroy() {
    }

    // --------------------------------------------------------------------- //
    // MessageReceiver

    @Override
    public void handleComponentData(final ByteBuf data) {
        ModSillyBee.getLogger().warn("Received a message for a component that does not implement handleComponentData(): {} ", getClass());
    }

    // --------------------------------------------------------------------- //

    /**
     * Get the list of all components attached to this component's entity.
     * <p>
     * This includes the component itself.
     *
     * @return the list of all components on the entity.
     */
    public Iterable<Component> getComponents() {
        return getManager().getComponents(getEntity());
    }

    /**
     * Get a component of the specified type attached to this component's entity.
     * <p>
     * This may be the component itself.
     *
     * @param clazz the type of the component to get.
     * @param <T>   the generic type of the component to get.
     * @return a component of the specified type, if any.
     */
    public <T> Optional<T> getComponent(final Class<T> clazz) {
        return getManager().getComponent(getEntity(), clazz);
    }

    /**
     * Get a list of all components of the specified type attached to this
     * component's entity.
     * <p>
     * This may include the component itself.
     *
     * @param clazz the type of the components to get.
     * @param <T>   the generic type of the components to get.
     * @return the list components of the specified type; may be empty.
     */
    public <T> Iterable<T> getComponents(final Class<T> clazz) {
        return getManager().getComponents(getEntity(), clazz);
    }

    // --------------------------------------------------------------------- //

    /**
     * Get whether this is a valid component.
     * <p>
     * A component is invalid if it no longer exists in a manager.
     *
     * @return <code>true</code> if the component is valid; <code>false</code> otherwise.
     */
    public boolean isValid() {
        return getManager().hasComponent(this);
    }

    /**
     * Get the world this component's entity lives in.
     * <p>
     * This requires a {@link Location} component to be attached to this
     * component's entity. This should always be the case.
     *
     * @return the world the component's entity lives in.
     */
    public World getWorld() {
        final Optional<Location> location = getComponent(Location.class);
        if (location.isPresent()) {
            return location.get().getWorld();
        } else {
            throw new IllegalStateException("Not in any world.");
        }
    }

    /**
     * Flag the entity the component belongs to as changed.
     * <p>
     * This will notify any {@link ChangeListener}s attached to the entity.
     * This is typically used to indicate that the entity needs to be saved
     * next time the world is saved (which is required for block based
     * entities).
     */
    public void markChanged() {
        getComponents(ChangeListener.class).forEach(ChangeListener::markChanged);
    }

    // --------------------------------------------------------------------- //

    /**
     * Send a data packet to the remote representation of this component.
     * <p>
     * When called on the client, will send the data to server representation
     * of the component. When called on the server, will send the data to all
     * client representations of this component that are within {@link #getMessageRange()}
     * of the entity.
     *
     * @param data the data to send to the remote representation.
     */
    public void sendData(final ByteBuf data) {
        final World world = getWorld();
        final int dimension = world.provider.getDimension();

        if (world.isRemote) {
            Network.INSTANCE.getWrapper().sendToServer(new MessageComponentData(dimension, getId(), data));
        } else {
            final Optional<Location> maybeLocation = getComponent(Location.class);
            assert maybeLocation.isPresent() : "This should be impossible; getWorld() should have failed before getting here.";
            final Location location = maybeLocation.get();
            final Vec3d pos = location.getPositionVector();
            final NetworkRegistry.TargetPoint targetPoint = new NetworkRegistry.TargetPoint(dimension, pos.xCoord, pos.yCoord, pos.zCoord, getMessageRange());
            Network.INSTANCE.getWrapper().sendToAllAround(new MessageComponentData(dimension, getId(), data), targetPoint);
        }
    }

    /**
     * The maximum distance of a player to this entity for a data packet sent
     * via {@link #sendData(ByteBuf)} to be sent to them.
     *
     * @return the maximum distance for packet receivers.
     */
    protected double getMessageRange() {
        return 64.0;
    }
}
