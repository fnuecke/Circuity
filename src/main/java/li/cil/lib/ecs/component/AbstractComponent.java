package li.cil.lib.ecs.component;

import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.api.ecs.component.Location;
import li.cil.lib.api.ecs.component.event.ChangeListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.stream.Stream;

public abstract class AbstractComponent implements Component {
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

    public Iterable<Component> getComponents() {
        return getManager().getComponents(getEntity());
    }

    public <T> Optional<T> getComponent(final Class<T> clazz) {
        return getManager().getComponent(getEntity(), clazz);
    }

    public <T> Stream<T> getComponents(final Class<T> clazz) {
        return getManager().getComponents(getEntity(), clazz);
    }

    // --------------------------------------------------------------------- //

    public World getWorld() {
        final Optional<Location> transform = getComponent(Location.class);
        if (transform.isPresent()) {
            return transform.get().getWorld();
        } else {
            throw new IllegalStateException("Not in any world.");
        }
    }

    public void markChanged() {
        final Optional<ChangeListener> listener = getComponent(ChangeListener.class);
        listener.ifPresent(ChangeListener::markChanged);
    }
}
