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
        final Optional<Location> location = getComponent(Location.class);
        location.ifPresent(l -> l.getWorld().markChunkDirty(l.getPosition(), null));
    }
}
