package li.cil.lib.capabilities;

import li.cil.lib.api.ecs.entity.EntityContainer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public final class CapabilityEntityContainer {
    @CapabilityInject(EntityContainer.class)
    public static Capability<EntityContainer> ENTITY_CONTAINER_CAPABILITY;

    // --------------------------------------------------------------------- //

    public static void register() {
        CapabilityManager.INSTANCE.register(EntityContainer.class, new NullStorage<>(), NullFactory::create);
    }

    // --------------------------------------------------------------------- //

    private CapabilityEntityContainer() {
    }
}
