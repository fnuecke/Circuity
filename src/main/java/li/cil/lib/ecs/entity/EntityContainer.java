package li.cil.lib.ecs.entity;

import li.cil.lib.api.ecs.component.Component;
import li.cil.lib.api.ecs.manager.EntityComponentManager;

import java.util.Optional;

public interface EntityContainer {
    EntityComponentManager getManager();

    long getEntity();

    <T extends Component> T addComponent(Class<T> clazz);

    Iterable<Component> getComponents();

    <T> Iterable<T> getComponents(Class<T> clazz);

    <T> Optional<T> getComponent(Class<T> clazz);
}
