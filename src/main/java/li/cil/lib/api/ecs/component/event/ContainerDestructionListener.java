package li.cil.lib.api.ecs.component.event;

import li.cil.lib.api.ecs.component.Component;

/**
 * When a component implements this interface, it will be notified when its
 * container is destroyed.
 * <p>
 * This includes a block containing the entity to which this component belongs
 * getting broken, or the entity containing it getting killed.
 * <p>
 * Used to drop items of the block's entity's inventory, for example.
 */
public interface ContainerDestructionListener extends Component {
    void handleContainerDestruction();
}
