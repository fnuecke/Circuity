package li.cil.lib.ecs.component;

import li.cil.lib.api.ecs.component.Location;
import li.cil.lib.api.ecs.component.event.ChangeListener;
import li.cil.lib.api.ecs.manager.EntityComponentManager;

import java.util.Optional;

public final class ChunkNotifyingChangeListener extends AbstractComponent implements ChangeListener {
    public ChunkNotifyingChangeListener(final EntityComponentManager manager, final long entity, final long id) {
        super(manager, entity, id);
    }

    // --------------------------------------------------------------------- //
    // ChangeListener

    @Override
    public void markChanged() {
        final Optional<Location> transform = getComponent(Location.class);
        transform.ifPresent(tf -> tf.getWorld().markChunkDirty(tf.getPosition(), null));
    }
}
