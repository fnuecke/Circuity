package li.cil.lib.synchronization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.ecs.manager.event.ComponentChangeListener;
import li.cil.lib.api.synchronization.SynchronizationManagerServer;
import li.cil.lib.api.synchronization.SynchronizedValue;
import li.cil.lib.network.Network;
import li.cil.lib.network.message.MessageInitialize;
import li.cil.lib.network.message.MessageSynchronizeValue;
import li.cil.lib.network.message.MessageTypeInfo;
import li.cil.lib.network.message.MessageTypeInfoList;
import li.cil.lib.util.ReflectionUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public final class SynchronizationManagerServerImpl extends AbstractSynchronizationManager implements SynchronizationManagerServer, ComponentChangeListener {
    /**
     * List of all components we're currently tracking for at least one client.
     */
    private final Map<Component, TrackingInfo> trackedComponents = new WeakHashMap<>();

    /**
     * Mapping of sync value to component, to allow getting back at the components
     * when processing dirty values.
     */
    private final Map<SynchronizedValue, WeakReference<Component>> trackedValues = new WeakHashMap<>();

    private final Object trackingLock = new Object();
    private final Object dirtyLock = new Object();

    /**
     * List of values that changed since the last update and need to be synchronized.
     */
    private Map<SynchronizedValue, List<Object>> dirtyValues = new HashMap<>();
    private Map<SynchronizedValue, List<Object>> dirtyValuesProcessing = new HashMap<>();

    // --------------------------------------------------------------------- //

    public SynchronizationManagerServerImpl() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    // --------------------------------------------------------------------- //

    /**
     * Begin tracking an entity for a client.
     *
     * @param client  the client to track the entity for.
     * @param manager the manager holding the entity.
     * @param entity  the ID of the entity.
     */
    public void subscribeEntity(final NetHandlerPlayServer client, final EntityComponentManager manager, final long entity) {
        synchronized (trackingLock) {
            for (final Component component : manager.getComponents(entity)) {
                subscribeComponent(client, component);
            }
        }
    }

    /**
     * Begin tracking a component for a client.
     *
     * @param client    the client to track the component for.
     * @param component the component to track.
     */
    public void subscribeComponent(final NetHandlerPlayServer client, final Component component) {
        synchronized (trackingLock) {
            final TrackingInfo info = trackedComponents.computeIfAbsent(component, this::beginTracking);
            if (info != null) {
                info.clients.add(client);
            }
        }
    }

    /**
     * Stop tracking an entity for a client.
     *
     * @param client  the client to stop tracking the entity for.
     * @param manager the manager holding the entity.
     * @param entity  the ID of the entity.
     */
    public void unsubscribeEntity(final NetHandlerPlayServer client, final EntityComponentManager manager, final long entity) {
        synchronized (trackingLock) {
            manager.getComponents(entity).forEach(component -> unsubscribeComponent(client, manager, component.getId()));
        }
    }

    /**
     * Stop tracking a component for a client.
     * <p>
     * This is a fallback used by clients when they receive sync data for
     * a component that they do not know (so they cannot ask to unsubscribe
     * from the entity the component belongs to).
     *
     * @param client      the client to stop tracking the component for.
     * @param manager     the manager holding the component.
     * @param componentId the ID of the component.
     */
    public void unsubscribeComponent(final NetHandlerPlayServer client, final EntityComponentManager manager, final long componentId) {
        final Component component = manager.getComponent(componentId);
        if (component != null) {
            synchronized (trackingLock) {
                final TrackingInfo info = trackedComponents.get(component);
                if (info != null) {
                    info.clients.remove(client);
                    if (info.clients.isEmpty()) {
                        stopTracking(component, info);
                    }
                }
            }
        }
    }

    /**
     * Get a serialized representation of the synchronized values of the specified component.
     * <p>
     * This re-uses the lookup table we keep for tracked components anyway when collecting
     * values for the initializing message sent to clients when beginning to track values.
     *
     * @param component the component to get the synchronized values of.
     * @return the list of serialized values.
     */
    @Nullable
    public NBTTagList getAllFieldValues(final Component component) {
        final TrackingInfo info = trackedComponents.get(component);
        if (info != null) {
            final NBTTagList componentNbt = new NBTTagList();
            for (final Map.Entry<SynchronizedValue, Field> entry : info.fields.entrySet()) {
                componentNbt.appendTag(buildValueNbt(entry.getValue(), entry.getKey(), null));
            }
            return componentNbt;
        }
        return null;
    }

    /**
     * Called regularly to check for changed fields to be synchronized to clients
     * and perform cleanup of lists.
     */
    public void update() {
        final Map<NetHandlerPlayServer, Map<World, Map<Component, NBTTagList>>> changes = new HashMap<>();

        /* Swap out the list of dirty values. Note that not keeping the lock during the writing
           operation below means that the values may in fact change again during this, leading
           to them being present in the dirty list for the next update again, even though their
           new value has already been synchronized. This is not a big issue, bandwidth-wise,
           however, and reduces lock contention, so in general it's worth it. */
        synchronized (dirtyLock) {
            assert (dirtyValuesProcessing.isEmpty());
            final Map<SynchronizedValue, List<Object>> tempValues = dirtyValues;
            dirtyValues = dirtyValuesProcessing;
            dirtyValuesProcessing = tempValues;
        }

        synchronized (trackingLock) {
            for (final Map.Entry<SynchronizedValue, List<Object>> entry : dirtyValuesProcessing.entrySet()) {
                final SynchronizedValue value = entry.getKey();
                final List<Object> tokens = entry.getValue();

                // We can get dirty values that are not tracked any longer if we stopped tracking
                // after it was added to the dirty list.
                final WeakReference<Component> componentRef = trackedValues.get(value);
                if (componentRef != null) {
                    // Component should not be null if the Synchronized value still exists, but
                    // someone might still keep a reference on it after the component went out
                    // of scope.
                    final Component component = componentRef.get();
                    if (component != null) {
                        // Tracking info can be null if there are no fields to be tracked in
                        // the component. In that case we should never have been registered as
                        // listener, but better safe than sorry...
                        final TrackingInfo info = trackedComponents.get(component);
                        if (info != null) {
                            // See if there are any clients even still tracking this value's
                            // component, if not clean up.
                            if (!info.clients.isEmpty()) {
                                final EntityComponentManager manager = component.getManager();
                                final World world = SillyBeeAPI.manager.getWorld(manager, false);

                                // Someone is still interested in the change!
                                final NBTTagCompound valueNbt = buildValueNbt(info.fields.get(value), value, tokens);
                                for (final NetHandlerPlayServer client : info.clients) {
                                    changes.computeIfAbsent(client, c -> new HashMap<>()).
                                            computeIfAbsent(world, c -> new HashMap<>()).
                                            computeIfAbsent(component, c -> new NBTTagList()).
                                            appendTag(valueNbt);
                                }
                            } else {
                                stopTracking(component, info);
                            }
                        } else {
                            stopTracking(value);
                        }
                    } else {
                        stopTracking(value);
                    }
                } else {
                    stopTracking(value);
                }
            }

            dirtyValuesProcessing.clear();
        }

        for (final Map.Entry<NetHandlerPlayServer, Map<World, Map<Component, NBTTagList>>> entry : changes.entrySet()) {
            final NetHandlerPlayServer client = entry.getKey();
            final Map<World, Map<Component, NBTTagList>> infos = entry.getValue();

            // TODO Package info by distance to player (via Location component on entity) where possible, send further away data less frequently.

            for (final Map.Entry<World, Map<Component, NBTTagList>> infoByWorld : infos.entrySet()) {
                final World world = infoByWorld.getKey();
                for (final Map.Entry<Component, NBTTagList> infoByComponent : infoByWorld.getValue().entrySet()) {
                    final Component component = infoByComponent.getKey();
                    final NBTTagList values = infoByComponent.getValue();

                    final MessageSynchronizeValue message = new MessageSynchronizeValue(world, component, values);
                    Network.INSTANCE.getWrapper().sendTo(message, client.playerEntity);
                }
            }
        }
    }

    // --------------------------------------------------------------------- //
    // AbstractSynchronizationManager

    @Override
    protected int registerType(final Class type) {
        final int typeId = super.registerType(type);
        final MessageTypeInfo message = new MessageTypeInfo(typeId, type);
        Network.INSTANCE.getWrapper().sendToAll(message);
        return typeId;
    }

    // --------------------------------------------------------------------- //
    //SynchronizationManagerServer

    @Override
    public void setDirty(final SynchronizedValue value, @Nullable final Object token) {
        synchronized (dirtyLock) {
            final List<Object> tokens = dirtyValues.computeIfAbsent(value, k -> token == null ? Collections.emptyList() : new ArrayList<>());
            if (token != null) {
                if (tokens == Collections.emptyList()) {
                    dirtyValues.put(value, new ArrayList<>()).add(token);
                } else {
                    tokens.add(token);
                }
            }
        }
    }

    @Override
    public void setDirtyAdvanced(final SynchronizedValue value, final Consumer<List<Object>> tokenUpdater) {
        synchronized (dirtyLock) {
            final List<Object> tokens = dirtyValues.computeIfAbsent(value, k -> new ArrayList<>());
            if (tokens == Collections.emptyList()) {
                tokenUpdater.accept(dirtyValues.put(value, new ArrayList<>()));
            } else {
                tokenUpdater.accept(tokens);
            }
        }
    }

    public int getTypeIdByValue(@Nullable final Object object) {
        return object == null ? -1 : getTypeIdByType(object.getClass());
    }

    public int getTypeIdByType(@Nullable final Class type) {
        return type == null ? -1 : idByType.computeIfAbsent(type, this::registerType);
    }

    // --------------------------------------------------------------------- //
    // ComponentChangeListener

    @Override
    public void onComponentAdded(final Component component) {
        final World world = SillyBeeAPI.manager.getWorld(component.getManager(), false);
        if (world == null) {
            return;
        }

        synchronized (trackingLock) {
            final TrackingInfo oldInfo = findTrackingInfo(component.getManager().getComponents(component.getEntity()));
            if (oldInfo != null) {
                final NBTTagList componentsNbt = new NBTTagList();
                componentsNbt.appendTag(MessageInitialize.getComponentNBT(component));
                final MessageInitialize message = new MessageInitialize(world.provider.getDimension(), component.getEntity(), componentsNbt);

                for (final NetHandlerPlayServer client : oldInfo.clients) {
                    subscribeComponent(client, component);
                    Network.INSTANCE.getWrapper().sendTo(message, client.playerEntity);
                }
            }
        }
    }

    @Override
    public void onComponentRemoved(final Component component) {
        synchronized (trackingLock) {
            final TrackingInfo info = trackedComponents.remove(component);
            if (info != null) {
                info.fields.keySet().forEach(trackedValues::remove);
            }
        }
    }

    // --------------------------------------------------------------------- //

    @SubscribeEvent
    public void onClientConnected(final FMLNetworkEvent.ServerConnectionFromClientEvent event) {
        final NetHandlerPlayServer connection = (NetHandlerPlayServer) event.getHandler();
        final MessageTypeInfoList message = new MessageTypeInfoList(typeById);

        // GLORIOUS HACKS
        connection.playerEntity.connection = connection;
        Network.INSTANCE.getWrapper().sendTo(message, connection.playerEntity);
        connection.playerEntity.connection = null;
    }
    // --------------------------------------------------------------------- //

    private static NBTTagCompound buildValueNbt(final Field field, final SynchronizedValue value, @Nullable final List<Object> tokens) {
        final NBTTagCompound compound = new NBTTagCompound();
        compound.setString(FIELD_TAG, field.getName());
        final ByteBuf buffer = Unpooled.buffer();
        final PacketBuffer packet = new PacketBuffer(buffer);
        value.serialize(packet, tokens);
        buffer.capacity(buffer.readableBytes());
        compound.setByteArray(VALUE_TAG, buffer.array());
        return compound;
    }

    @Nullable
    private TrackingInfo findTrackingInfo(final Iterable<Component> components) {
        for (final Component component : components) {
            final TrackingInfo info = trackedComponents.get(component);
            if (info != null) {
                return info;
            }
        }
        return null;
    }

    @Nullable
    private TrackingInfo beginTracking(final Component component) {
        final List<Field> fields = ReflectionUtil.getFieldsByType(component.getClass(), SynchronizedValue.class);
        if (!fields.isEmpty()) {
            final Map<SynchronizedValue, Field> fieldMap = new WeakHashMap<>();
            for (final Field field : fields) {
                if (!Modifier.isFinal(field.getModifiers())) {
                    throw new IllegalArgumentException("Synchronized field " + field.getDeclaringClass().getName() + "." + field.getName() + " must be final.");
                }
                final SynchronizedValue value = ReflectionUtil.get(component, field);
                if (value == null) {
                    throw new IllegalArgumentException("Synchronized fields must not be null.");
                }
                value.setManager(this);
                trackedValues.put(value, new WeakReference<>(component)); // Locked in subscribeEntity
                fieldMap.put(value, field);
            }
            return new TrackingInfo(fieldMap);
        }
        return null;
    }

    private void stopTracking(final Component component, final TrackingInfo info) {
        trackedComponents.remove(component);
        info.fields.keySet().forEach(this::stopTracking);
    }

    private void stopTracking(final SynchronizedValue value) {
        value.setManager(null);
        trackedValues.remove(value);
    }

    private static final class TrackingInfo {
        final Map<SynchronizedValue, Field> fields;
        final Set<NetHandlerPlayServer> clients = Collections.newSetFromMap(new WeakHashMap<>());

        private TrackingInfo(final Map<SynchronizedValue, Field> fields) {
            this.fields = fields;
        }
    }
}
