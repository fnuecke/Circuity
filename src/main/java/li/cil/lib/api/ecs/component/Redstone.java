package li.cil.lib.api.ecs.component;

import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

/**
 * Component controlling redstone I/O of an entity.
 * <p>
 * An entity is expected to have none or exactly one of these. This is the
 * component type used by the block container to determine its redstone
 * input and output.
 *
 * @see li.cil.lib.ecs.component.RedstoneController
 * @see li.cil.lib.ecs.component.SidedRedstoneController
 */
public interface Redstone extends Component {
    /**
     * Get the current input signal on the specified side.
     * <p>
     * This is automatically kept up to date upon neighbor changes. It is also
     * synchronized to the client side, in case input specific state needs be
     * rendered.
     * <p>
     * When passing <code>null</code>, this will always return zero.
     * <p>
     * The specified side is relative to the entity. For example, when passing
     * {@link EnumFacing#UP} the input from the top face of the block the
     * entity is in is returned.
     *
     * @param side the side to get the redstone input signal for.
     * @return the current input on the specified side.
     */
    int getInput(@Nullable final EnumFacing side);

    /**
     * Get the current output signal on the specified side.
     * <p>
     * Depending on the implementation, the side may be ignored. The default
     * sided implementation of this interface treats passing <code>null</code>
     * as a "seventh side".
     * <p>
     * The specified side is relative to the entity. For example, when passing
     * {@link EnumFacing#UP} the input from the top face of the block the
     * entity is in is returned.
     *
     * @param side the side to get the current output signal for.
     * @return the current output on the specified side.
     */
    int getOutput(@Nullable final EnumFacing side);

    /**
     * Sets a new output signal for the specified side.
     * <p>
     * Depending on the implementation, the side may be ignored. The default
     * sided implementation of this interface treats passing <code>null</code>
     * as a "seventh side".
     * <p>
     * The specified side is relative to the entity. For example, when passing
     * {@link EnumFacing#UP} the input from the top face of the block the
     * entity is in is returned.
     *
     * @param side  the side to set the current output signal for.
     * @param value the new output signal to set for the specified side.
     */
    void setOutput(@Nullable final EnumFacing side, final int value);
}
