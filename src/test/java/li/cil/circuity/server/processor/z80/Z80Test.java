package li.cil.circuity.server.processor.z80;

import li.cil.circuity.common.ecs.component.BusDeviceRandomAccessMemory;
import li.cil.circuity.common.ecs.component.BusDeviceZ80Processor;
import li.cil.lib.Manager;
import li.cil.lib.Scheduler;
import li.cil.lib.Serialization;
import li.cil.lib.Synchronization;
import li.cil.lib.api.SillyBeeAPI;
import li.cil.lib.api.ecs.manager.EntityComponentManager;
import net.minecraft.init.Bootstrap;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;

public class Z80Test {
    @Before
    public void setup() {
        Loader.instance();
        Bootstrap.register();

        SillyBeeAPI.manager = Manager.INSTANCE;
        SillyBeeAPI.scheduler = Scheduler.INSTANCE;
        SillyBeeAPI.serialization = Serialization.INSTANCE;
        SillyBeeAPI.synchronization = Synchronization.INSTANCE;
    }

    @Test
    public void test() {
        final WorldSettings settings = new WorldSettings(0, GameType.CREATIVE, false, false, WorldType.FLAT);
        final WorldInfo info = new WorldInfo(settings, "test");
        final TestServer server = new TestServer();
        final WorldServer world = new WorldServer(server, new TestSaveHandler(info), info, 0, new Profiler());

        final EntityComponentManager manager = SillyBeeAPI.manager.getManager(world);

        final long entity = manager.addEntity();
        manager.addComponent(entity, TestLocation.class).setWorld(world);
        final TestRedstone redstone = manager.addComponent(entity, TestRedstone.class);

        // 64K
        manager.addComponent(entity, BusDeviceRandomAccessMemory.class).setSize(4 * 1024);
        manager.addComponent(entity, BusDeviceRandomAccessMemory.class).setSize(4 * 1024);
        manager.addComponent(entity, BusDeviceRandomAccessMemory.class).setSize(4 * 1024);
        manager.addComponent(entity, BusDeviceRandomAccessMemory.class).setSize(4 * 1024);
        manager.addComponent(entity, BusDeviceRandomAccessMemory.class).setSize(4 * 1024);
        manager.addComponent(entity, BusDeviceRandomAccessMemory.class).setSize(4 * 1024);
        manager.addComponent(entity, BusDeviceRandomAccessMemory.class).setSize(4 * 1024);
        manager.addComponent(entity, BusDeviceRandomAccessMemory.class).setSize(4 * 1024);
        manager.addComponent(entity, BusDeviceRandomAccessMemory.class).setSize(4 * 1024);
        manager.addComponent(entity, BusDeviceRandomAccessMemory.class).setSize(4 * 1024);
        manager.addComponent(entity, BusDeviceRandomAccessMemory.class).setSize(4 * 1024);
        manager.addComponent(entity, BusDeviceRandomAccessMemory.class).setSize(4 * 1024);
        manager.addComponent(entity, BusDeviceRandomAccessMemory.class).setSize(4 * 1024);
        manager.addComponent(entity, BusDeviceRandomAccessMemory.class).setSize(4 * 1024);
        manager.addComponent(entity, BusDeviceRandomAccessMemory.class).setSize(4 * 1024);
        manager.addComponent(entity, BusDeviceRandomAccessMemory.class).setSize(4 * 1024);

        final BusDeviceZ80Processor cpu = manager.addComponent(entity, BusDeviceZ80Processor.class);
        final TestBusController controller = manager.addComponent(entity, TestBusController.class);
        final TestHooks hooks = manager.addComponent(entity, TestHooks.class);

        final Z80 z80 = ((BusDeviceZ80Processor.BusDeviceZ80Impl) cpu.getDevice()).z80;
        hooks.device.setCpu(z80);

        for (int i = 0; i < 10; i++) {
            tick();
        }

        redstone.setInput(15);
        z80.reset(0x100);

        while (z80.run(0)) {
            tick();
        }

        assertFalse(hooks.device.getOutput().contains("ERROR"));
    }

    private static final class TestServer extends MinecraftServer {
        public TestServer() {
            super(new File("nope"), null, new DataFixer(0), null, null, null, null);
            setPlayerList(new PlayerList(this) {
            });
        }

        @Override
        public boolean startServer() throws IOException {
            return false;
        }

        @Override
        public boolean canStructuresSpawn() {
            return false;
        }

        @Override
        public GameType getGameType() {
            return GameType.SURVIVAL;
        }

        @Override
        public EnumDifficulty getDifficulty() {
            return EnumDifficulty.NORMAL;
        }

        @Override
        public boolean isHardcore() {
            return false;
        }

        @Override
        public int getOpPermissionLevel() {
            return 0;
        }

        @Override
        public boolean shouldBroadcastRconToOps() {
            return false;
        }

        @Override
        public boolean shouldBroadcastConsoleToOps() {
            return false;
        }

        @Override
        public boolean isDedicatedServer() {
            return true;
        }

        @Override
        public boolean shouldUseNativeTransport() {
            return false;
        }

        @Override
        public boolean isCommandBlockEnabled() {
            return false;
        }

        @Override
        public String shareToLAN(final GameType type, final boolean allowCheats) {
            return null;
        }
    }

    private static final TickEvent.ServerTickEvent TICK_START = new TickEvent.ServerTickEvent(TickEvent.Phase.START);
    private static final TickEvent.ServerTickEvent TICK_END = new TickEvent.ServerTickEvent(TickEvent.Phase.END);

    private static void tick() {
        Manager.INSTANCE.handleServerTick(TICK_START);
        Scheduler.INSTANCE.handleServerTick(TICK_START);
        Manager.INSTANCE.handleServerTick(TICK_END);
        Scheduler.INSTANCE.handleServerTick(TICK_END);
    }
}