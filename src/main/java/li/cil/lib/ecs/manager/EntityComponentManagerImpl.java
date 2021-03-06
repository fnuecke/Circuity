package li.cil.lib.ecs.manager;

import com.google.common.base.Throwables;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import li.cil.circuity.ModCircuity;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.api.ecs.component.LateTickable;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import li.cil.lib.api.ecs.manager.event.ComponentChangeListener;
import li.cil.lib.api.ecs.manager.event.EntityChangeListener;
import li.cil.lib.common.Manager;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class EntityComponentManagerImpl implements EntityComponentManager {
    private static final Comparator<ITickable> TICKABLE_COMPARATOR = Comparator.comparing(t -> ((Component) t).getId());
    private static final Comparator<LateTickable> LATE_TICKABLE_COMPARATOR = Comparator.comparing(Component::getId);

    // --------------------------------------------------------------------- //

    private final ReentrantLock lock = new ReentrantLock();
    private long lastId = 0;
    private final TLongSet entities = new TLongHashSet();
    private final TLongObjectMap<List<Component>> componentsByEntity = new TLongObjectHashMap<>();
    private final TLongObjectMap<HashMap<Class<?>, List<Component>>> componentsByEntityAndType = new TLongObjectHashMap<>();
    private final TLongObjectMap<Component> componentsById = new TLongObjectHashMap<>();
    private final HashMap<Class<?>, List<Component>> componentsByType = new HashMap<>();
    private final List<ITickable> updatingComponents = new ArrayList<>();
    private final Set<ITickable> addedUpdatingComponents = new HashSet<>();
    private final Set<ITickable> removedUpdatingComponents = new HashSet<>();
    private final List<LateTickable> lateUpdatingComponents = new ArrayList<>();
    private final Set<LateTickable> addedLateUpdatingComponents = new HashSet<>();
    private final Set<LateTickable> removedLateUpdatingComponents = new HashSet<>();
    private final Set<EntityChangeListener> entityChangeListeners = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<ComponentChangeListener> componentChangeListeners = Collections.newSetFromMap(new WeakHashMap<>());

    // --------------------------------------------------------------------- //

    /**
     * Get a lock to synchronize <em>some</em> interaction with the manager.
     * <p>
     * In particular, this is used to synchronize updating components with
     * removal of components and entities with network messages (for both
     * sides) and with adding of components and entities (for the client side).
     *
     * @return the lock to use for synchronized operations.
     */
    public ReentrantLock getLock() {
        return lock;
    }

    /**
     * Adds a component with the specified id.
     * <p>
     * Should never be used directly, used in synchronization to create entities.
     *
     * @param entity the id of the entity to create.
     * @return <code>true</code> if the entity was created; <code>false</code> if it already existed.
     */
    public boolean addEntity(final long entity) {
        lock.lock();
        try {
            if (entities.contains(entity)) {
                return false;
            }
            entities.add(entity);

            entityChangeListeners.forEach(l -> l.handleEntityAdded(this, entity));
        } finally {
            lock.unlock();
        }

        return true;
    }

    /**
     * Adds a component with the specified id.
     * <p>
     * Should never be used directly, used in synchronization to create entities.
     *
     * @param entity the entity to attach the component to.
     * @param id     the id of the component to crate.
     * @param clazz  the type of the component to create.
     * @param <T>    the type of the component to create.
     * @return the created (or existing) component.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T addComponent(final long entity, final long id, final Class<T> clazz) {
        lock.lock();
        try {
            validateEntity(entity);

            if (hasComponent(id)) {
                final Component component = componentsById.get(id);
                if (component.getEntity() != entity) {
                    throw new IllegalArgumentException("Component with this ID already exists but belongs to a different entity.");
                }
                if (component.getClass() != clazz) {
                    throw new IllegalArgumentException("Component with this ID already exists but has a different type.");
                }
                return (T) component;
            }

            try {
                final Constructor<T> constructor = clazz.getConstructor(EntityComponentManager.class, long.class, long.class);
                if (constructor == null) {
                    throw new IllegalArgumentException("Component type does not have constructor with required signature (EntityComponentManager, long, long).");
                }
                constructor.setAccessible(true);
                final T component = constructor.newInstance(this, entity, id);
                componentsById.
                        put(id, component);
                collectTypes(clazz, type -> componentsByType.
                        computeIfAbsent(type, k -> new ArrayList<>()).
                        add(component));
                computeIfAbsent(componentsByEntity, entity, ArrayList::new).
                        add(component);
                collectTypes(clazz, type -> computeIfAbsent(componentsByEntityAndType, entity, HashMap::new).
                        computeIfAbsent(type, k -> new ArrayList<>()).
                        add(component));

                if (component instanceof ITickable) {
                    addedUpdatingComponents.add((ITickable) component);
                }
                if (component instanceof LateTickable) {
                    addedLateUpdatingComponents.add((LateTickable) component);
                }

                component.onCreate();

                // Unlikely, but component may have decided to self-destruct in onCreate.
                if (hasComponent(id)) {
                    componentChangeListeners.forEach(l -> l.onComponentAdded(component));
                }

                return component;
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw Throwables.propagate(e);
            }
        } finally {
            lock.unlock();
        }
    }

    private static <T> T computeIfAbsent(final TLongObjectMap<T> map, final long key, final Supplier<T> supplier) {
        T value = map.get(key);
        if (value == null) {
            map.put(key, value = supplier.get());
        }
        return value;
    }

    // --------------------------------------------------------------------- //
    // EntityComponentManager

    @Override
    public long addEntity() throws UnsupportedOperationException {
        requireServerSide();

        lock.lock();
        try {
            final long entity = nextId();
            addEntity(entity);
            return entity;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean hasEntity(final long entity) {
        lock.lock();
        try {
            return entities.contains(entity);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean removeEntity(final long entity) {
        lock.lock();
        try {
            // Remove at the end, so that the entity still exists while components are removed.
            if (!entities.contains(entity)) {
                return false;
            }

            if (componentsByEntity.containsKey(entity)) {
                final List<Component> components = componentsByEntity.get(entity);
                while (!components.isEmpty()) {
                    removeComponent(components.get(components.size() - 1));
                }
                componentsByEntity.remove(entity);
            }
            if (componentsByEntityAndType.containsKey(entity)) {
                componentsByEntityAndType.remove(entity);
            }

            entities.remove(entity);

            entityChangeListeners.forEach(l -> l.handleEntityRemoved(this, entity));
        } finally {
            lock.unlock();
        }

        return true;
    }

    @Override
    public <T extends Component> T addComponent(final long entity, final Class<T> clazz) throws UnsupportedOperationException {
        requireServerSide();

        lock.lock();
        try {
            validateEntity(entity);
            final long id = nextId();
            return addComponent(entity, id, clazz);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean hasComponent(final long component) {
        lock.lock();
        try {
            return componentsById.containsKey(component);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean hasComponent(final Component component) {
        lock.lock();
        try {
            return component == componentsById.get(component.getId());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean removeComponent(final Component component) throws UnsupportedOperationException {
        lock.lock();
        try {
            if (!hasComponent(component.getId())) {
                return false;
            }

            component.onDestroy();

            final long entity = component.getEntity();
            componentsById.remove(component.getId());
            collectTypes(component.getClass(), type -> componentsByType.
                    computeIfPresent(type, (k, list) -> {
                        list.remove(component);
                        return list.isEmpty() ? null : list;
                    }));
            componentsByEntity.get(entity).remove(component);
            componentsByEntityAndType.get(entity).get(component.getClass()).remove(component);

            if (component instanceof ITickable) {
                removedUpdatingComponents.add((ITickable) component);
            }
            if (component instanceof LateTickable) {
                removedLateUpdatingComponents.add((LateTickable) component);
            }

            componentChangeListeners.forEach(l -> l.onComponentRemoved(component));
        } finally {
            lock.unlock();
        }

        return true;
    }

    @Nullable
    @Override
    public Component getComponent(final long id) {
        lock.lock();
        try {
            return componentsById.get(id);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Iterable<T> getComponents(final Class<T> clazz) {
        lock.lock();
        try {
            if (componentsByType.containsKey(clazz)) {
                return (Iterable<T>) componentsByType.get(clazz);
            } else {
                return Collections.emptyList();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <T> Optional<T> getComponent(final long entity, final Class<T> clazz) {
        lock.lock();
        try {
            final Iterable<T> components = getComponents(entity, clazz);
            final Iterator<T> iterator = components.iterator();
            if (iterator.hasNext()) {
                return Optional.of(iterator.next());
            } else {
                return Optional.empty();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Iterable<T> getComponents(final long entity, final Class<T> clazz) {
        lock.lock();
        try {
            final HashMap<Class<?>, List<Component>> componentTypes = componentsByEntityAndType.get(entity);
            if (componentTypes != null) {
                return (Iterable<T>) componentTypes.getOrDefault(clazz, Collections.emptyList());
            } else {
                return Collections.emptyList();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterable<Component> getComponents(final long entity) {
        lock.lock();
        try {
            final List<Component> components = componentsByEntity.get(entity);
            if (components != null) {
                return components;
            } else {
                return Collections.emptyList();
            }
        } finally {
            lock.unlock();
        }
    }

    // --------------------------------------------------------------------- //

    /**
     * Called at the beginning of each tick from {@link Manager#handleClientTick(TickEvent.ClientTickEvent)}
     * or {@link Manager#handleServerTick(TickEvent.ServerTickEvent)} (depending on which side this manager is on).
     * <p>
     * Processes lists of added and removed components, then updates all
     * tickable components currently managed by this manager.
     */
    public void update() {
        updateTickables(addedUpdatingComponents, removedUpdatingComponents, updatingComponents, TICKABLE_COMPARATOR, ITickable::update);
    }

    /**
     * Called at the end of each tick from {@link Manager#handleClientTick(TickEvent.ClientTickEvent)}
     * or {@link Manager#handleServerTick(TickEvent.ServerTickEvent)} (depending on which side this manager is on).
     * <p>
     * Processes lists of added and removed components, then updates all
     * tickable components currently managed by this manager.
     */
    public void lateUpdate() {
        updateTickables(addedLateUpdatingComponents, removedLateUpdatingComponents, lateUpdatingComponents, LATE_TICKABLE_COMPARATOR, LateTickable::lateUpdate);
    }

    // --------------------------------------------------------------------- //

    public void addEntityChangeListener(final EntityChangeListener listener) {
        entityChangeListeners.add(listener);
    }

    public boolean removeEntityChangeListener(final EntityChangeListener listener) {
        return entityChangeListeners.remove(listener);
    }

    public void addComponentChangeListener(final ComponentChangeListener listener) {
        componentChangeListeners.add(listener);
    }

    public boolean removeComponentChangeListener(final ComponentChangeListener listener) {
        return componentChangeListeners.remove(listener);
    }

    // --------------------------------------------------------------------- //

    private void requireServerSide() throws UnsupportedOperationException {
        if (isClientSide()) {
            throw new UnsupportedOperationException("Cannot create entities or components on the client side.");
        }
    }

    private boolean isClientSide() {
        final World world = SillyBeeAPI.manager.getWorld(this, false);
        return world == null || world.isRemote;
    }

    private long nextId() {
        return ++lastId;
    }

    private void validateEntity(final long entity) {
        if (!entities.contains(entity)) {
            throw new IllegalArgumentException("Invalid entity.");
        }
    }

    private <T> void updateTickables(final Set<T> added, final Set<T> removed, final List<T> current, final Comparator<T> comparator, final Consumer<T> updater) {
        lock.lock();
        try {
            for (final T component : added) {
                final int index = Collections.binarySearch(current, component, comparator);
                assert index < 0 : "Inserting tickable that is already in the list!";
                current.add(~index, component);
            }
            added.clear();

            for (final T component : removed) {
                final int index = Collections.binarySearch(current, component, comparator);
                assert index >= 0 : "Removing tickable that is not in the list!";
                current.remove(index);
            }
            removed.clear();
        } finally {
            lock.unlock();
        }

        for (final T component : current) {
            if (!removed.contains(component)) {
                try {
                    updater.accept(component);
                } catch (final Throwable t) {
                    ModCircuity.getLogger().catching(t);
                }
            }
        }
    }

    private static void collectTypes(@Nullable final Class<?> clazz, final Consumer<Class<?>> consumer) {
        if (clazz == null) {
            return;
        }
        consumer.accept(clazz);
        for (final Class<?> interfaceClass : clazz.getInterfaces()) {
            consumer.accept(interfaceClass);
        }
        collectTypes(clazz.getSuperclass(), consumer);
    }
}
