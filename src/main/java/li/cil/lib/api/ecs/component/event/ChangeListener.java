package li.cil.lib.api.ecs.component.event;

import li.cil.lib.api.ecs.component.Component;

/**
 * Used by the default component implementation's {@link li.cil.lib.ecs.component.AbstractComponent#markChanged()}.
 * <p>
 * This is typically invoked when a components wishes to mark its entity as
 * changed in a way that requires saving in the next world save. For the most
 * part this is only needed by tile entity based entities. When using the
 * {@link li.cil.lib.tileentity.TileEntityEntityContainer}, you do not need
 * to worry about this, as a default implementation is automatically added,
 * forwarding a call to this interface to the containing chunk.
 */
public interface ChangeListener extends Component {
    void markChanged();
}
