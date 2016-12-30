package li.cil.lib.ecs.entity;

import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.api.ecs.entity.EntityContainer;

import java.util.Collections;
import java.util.Optional;

public interface EntityContainerProxy extends EntityContainer {
    @Override
    default <T extends Component> T addComponent(final Class<T> clazz) {
        return getManager().addComponent(getEntity(), clazz);
    }

    @Override
    default Iterable<Component> getComponents() {
        final long entity = getEntity();
        if (entity != 0) {
            return getManager().getComponents(getEntity());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    default <T> Iterable<T> getComponents(final Class<T> clazz) {
        final long entity = getEntity();
        if (entity != 0) {
            return getManager().getComponents(getEntity(), clazz);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    default <T> Optional<T> getComponent(final Class<T> clazz) {
        final long entity = getEntity();
        if (entity != 0) {
            return getManager().getComponent(getEntity(), clazz);
        } else {
            return Optional.empty();
        }
    }
}
