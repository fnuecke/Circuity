package li.cil.lib.ecs.entity;

import li.cil.lib.api.ecs.component.Component;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public interface EntityContainerProxy extends EntityContainer {
    @Nullable
    @Override
    default <T extends Component> T addComponent(final Class<T> clazz) {
        return getManager().addComponent(getEntity(), clazz);
    }

    @Override
    default Iterable<Component> getComponents() {
        return getManager().getComponents(getEntity());
    }

    @Override
    default <T> Stream<T> getComponents(final Class<T> clazz) {
        final long entity = getEntity();
        if (entity != 0) {
            return getManager().getComponents(getEntity(), clazz);
        } else {
            return Stream.empty();
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
