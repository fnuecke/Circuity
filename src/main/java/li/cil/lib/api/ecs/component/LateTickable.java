package li.cil.lib.api.ecs.component;

/**
 * Like {@link net.minecraft.util.ITickable}, but called at the end of a server
 * frame instead of at the start.
 */
public interface LateTickable extends Component {
    void lateUpdate();
}
