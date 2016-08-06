package li.cil.lib.synchronization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.ecs.manager.event.EntityChangeListener;
import li.cil.lib.api.synchronization.SynchronizationListener;
import li.cil.lib.api.synchronization.SynchronizationManagerClient;
import li.cil.lib.api.synchronization.SynchronizedValue;
import li.cil.lib.ecs.manager.EntityComponentManagerImpl;
import li.cil.lib.network.Network;
import li.cil.lib.network.message.MessageSubscribe;
import li.cil.lib.network.message.MessageUnsubscribeEntity;
import li.cil.lib.util.ReflectionUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class SynchronizationManagerClientImpl extends AbstractSynchronizationManager implements SynchronizationManagerClient, EntityChangeListener {
    private final Map<Class, Map<String, Field>> fieldsByName = new HashMap<>();

    // --------------------------------------------------------------------- //

    /**
     * Called from {@link li.cil.lib.network.handler.MessageHandlerSynchronizeValue}
     * when new values for a component were received.
     *
     * @param component the component that new values were received for.
     * @param values    the values received for the component.
     */
    public void update(final Component component, final NBTTagList values) {
        final Class componentClass = component.getClass();
        final Map<String, Field> componentFields = fieldsByName.computeIfAbsent(componentClass, k -> new HashMap<>());
        final List<SynchronizedValue> synchronizedValues = new ArrayList<>();

        for (int i = 0; i < values.tagCount(); i++) {
            final NBTTagCompound valueNbt = values.getCompoundTagAt(i);
            final String fieldName = valueNbt.getString(FIELD_TAG);

            // Extra check to avoid building closure in getFieldByName when value is already known.
            final Field field;
            if (componentFields.containsKey(fieldName)) {
                field = componentFields.get(fieldName);
            } else {
                field = componentFields.computeIfAbsent(fieldName, getFieldByName(component));
            }

            final SynchronizedValue clientValue = ReflectionUtil.get(component, field);
            if (clientValue != null) {
                final ByteBuf buffer = Unpooled.wrappedBuffer(valueNbt.getByteArray(VALUE_TAG));
                final PacketBuffer packet = new PacketBuffer(buffer);
                clientValue.deserialize(packet);
                synchronizedValues.add(clientValue);
            }
        }

        if (component instanceof SynchronizationListener) {
            final SynchronizationListener listener = (SynchronizationListener) component;
            listener.onSynchronize(synchronizedValues);
        }
    }

    /**
     * Called from {@link li.cil.lib.network.handler.MessageHandlerTypeInfoList}
     * with an initial list of type information after joining a server. This
     * list is used during synchronization to provide more compressed transferal
     * of type information.
     *
     * @param types the list of types received.
     */
    public void registerTypes(final List<Class> types) {
        typeById.clear();
        idByType.clear();
        types.forEach(this::registerType);
    }

    /**
     * Called from {@link li.cil.lib.network.handler.MessageHandlerTypeInfo}
     * when additional type information is received from the server (due to a
     * previously unmapped type needing to be synchronized to the client).
     *
     * @param type   the type to add a numeric mapping for.
     * @param typeId the numeric ID of the type.
     */
    public void registerType(final Class type, final int typeId) {
        while (typeById.size() <= typeId) {
            typeById.add(null);
        }
        typeById.set(typeId, type);
        idByType.put(type, typeId);
    }

    // --------------------------------------------------------------------- //
    // SynchronizationManagerClient

    @Override
    public void subscribe(final EntityComponentManager manager, final long entity) {
        // We need/want to cast here, because we don't want this method in the public API.
        ((EntityComponentManagerImpl) manager).addEntity(entity);
    }

    @Override
    public void unsubscribe(final EntityComponentManager manager, final long entity) {
        manager.removeEntity(entity);
    }

    @Nullable
    public Class getTypeByTypeId(final int id) {
        if (id == -1) {
            return null;
        }

        if (id < typeById.size()) {
            final Class type = typeById.get(id);
            if (type != null) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown ID.");
    }

    // --------------------------------------------------------------------- //
    // EntityChangeListener

    @Override
    public void handleEntityAdded(final EntityComponentManager manager, final long entity) {
        final World world = SillyBeeAPI.manager.getWorld(manager, true);
        if (world != null) {
            Network.INSTANCE.getWrapper().sendToServer(new MessageSubscribe(world.provider.getDimension(), entity));
        }
    }

    @Override
    public void handleEntityRemoved(final EntityComponentManager manager, final long entity) {
        final World world = SillyBeeAPI.manager.getWorld(manager, true);
        if (world != null) {
            Network.INSTANCE.getWrapper().sendToServer(new MessageUnsubscribeEntity(world.provider.getDimension(), entity));
        }
    }

    // --------------------------------------------------------------------- //

    private static Function<String, Field> getFieldByName(final Component component) {
        final List<Field> fields = ReflectionUtil.getFieldsByType(component.getClass(), SynchronizedValue.class);
        return fieldName -> {
            for (final Field field : fields) {
                if (field.getName().equals(fieldName)) {
                    return field;
                }
            }

            throw new IllegalArgumentException("Field '" + component.getClass().getName() + "." + fieldName + "' does not exist.");
        };
    }
}
