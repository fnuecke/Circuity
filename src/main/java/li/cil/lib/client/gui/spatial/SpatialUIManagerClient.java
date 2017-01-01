package li.cil.lib.client.gui.spatial;

import li.cil.lib.ModSillyBee;
import li.cil.lib.common.SpatialUI;
import li.cil.lib.api.gui.input.InputEvent;
import li.cil.lib.api.gui.spatial.SpatialUIClient;
import li.cil.lib.api.gui.spatial.SpatialUIProviderClient;
import li.cil.lib.api.gui.spatial.SpatialUIProviderServer;
import li.cil.lib.api.math.Vector2;
import li.cil.lib.common.gui.spatial.SpatialUIManagerServer;
import li.cil.lib.network.Network;
import li.cil.lib.network.message.MessageSpatialUISubscribe;
import li.cil.lib.network.message.MessageSpatialUIUnsubscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

import java.util.Objects;
import java.util.function.Consumer;

public final class SpatialUIManagerClient {
    public static final SpatialUIManagerClient INSTANCE = new SpatialUIManagerClient();

    private static final int SWITCH_DELAY = 100;

    // --------------------------------------------------------------------- //

    private final Object writeLock = new Object();
    private ICapabilityProvider currentTarget;
    private BlockPos currentBlockPos;
    private EnumFacing currentSide;
    private SpatialUIClient currentUI;
    private long lastValidTime;

    // --------------------------------------------------------------------- //

    public static void init() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    public void handleData(final NBTTagCompound data) {
        synchronized (writeLock) {
            if (currentUI != null) {
                try {
                    currentUI.handleData(data);
                } catch (final Throwable t) {
                    ModSillyBee.getLogger().error(t);
                }
            }
        }
    }

    public void close() {
        if (currentBlockPos == null && currentSide == null && currentTarget == null && currentUI == null) {
            return;
        }

        currentBlockPos = null;
        currentSide = null;
        currentTarget = null;
        lastValidTime = System.currentTimeMillis(); // Only re-scan periodically.
        synchronized (writeLock) {
            currentUI = null;
        }

        Network.INSTANCE.getWrapper().sendToServer(new MessageSpatialUIUnsubscribe());
    }

    // --------------------------------------------------------------------- //

    @SubscribeEvent
    public void handleDrawBlockHighlightEvent(final DrawBlockHighlightEvent event) {
        if (currentUI == null) {
            return;
        }

        final EntityPlayer player = event.getPlayer();
        final RayTraceResult target = event.getTarget();

        final Vec3d hitVec = target.hitVec == null ? Vec3d.ZERO : target.hitVec;
        final Vector2 mousePosition = map(hitVec, currentBlockPos, player.getHorizontalFacing());
        currentUI.handleInput(InputEvent.getPointerEvent(mousePosition));

        final Vec3d playerPos = new Vec3d(
                player.prevPosX + (player.posX - player.prevPosX) * event.getPartialTicks(),
                player.prevPosY + (player.posY - player.prevPosY) * event.getPartialTicks(),
                player.prevPosZ + (player.posZ - player.prevPosZ) * event.getPartialTicks());
        final Vec3d renderPos = new Vec3d(currentBlockPos.getX(), currentBlockPos.getY(), currentBlockPos.getZ()).addVector(-playerPos.xCoord, -playerPos.yCoord, -playerPos.zCoord);

        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();

        RenderHelper.disableStandardItemLighting();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 0);

        GlStateManager.translate(renderPos.xCoord + 0.5, renderPos.yCoord + 0.5, renderPos.zCoord + 0.5);
        setupMatrix(currentSide, player.getHorizontalFacing());

        currentUI.render();

        RenderHelper.enableStandardItemLighting();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();

        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void handleLeftClick(final PlayerInteractEvent.LeftClickBlock event) {
        handleClick(event, this::handleLeftClickClient);
    }

    @SubscribeEvent
    public void handleRightClick(final PlayerInteractEvent.RightClickBlock event) {
        handleClick(event, this::handleRightClickClient);
    }

    @SubscribeEvent
    public void handleClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            final EntityPlayer player = Minecraft.getMinecraft().player;
            final RayTraceResult target = Minecraft.getMinecraft().objectMouseOver;
            if (player == null || target == null) {
                close();
                return; // No ingame.
            }

            if (target.typeOfHit == RayTraceResult.Type.BLOCK) {
                final World world = player.getEntityWorld();
                final BlockPos blockPos = target.getBlockPos();
                final EnumFacing side = target.sideHit;
                final TileEntity tileEntity = world.isBlockLoaded(blockPos) ? world.getTileEntity(blockPos) : null;

                final boolean isValid = currentUI != null && Objects.equals(blockPos, currentBlockPos) && side == currentSide && Objects.equals(tileEntity, currentTarget);
                if (isValid) {
                    lastValidTime = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - lastValidTime > SWITCH_DELAY) {
                    close();

                    if (tileEntity == null) {
                        return;
                    }

                    currentBlockPos = blockPos;
                    currentSide = side;
                    currentTarget = tileEntity;

                    for (final SpatialUIProviderClient provider : SpatialUI.INSTANCE.getClientProviders()) {
                        if (provider.canProvideFor(player, tileEntity, currentSide)) {
                            final Class<? extends SpatialUIProviderServer> providerClass = SpatialUI.INSTANCE.getServerProviderClass(provider);
                            Network.INSTANCE.getWrapper().sendToServer(new MessageSpatialUISubscribe(world.provider.getDimension(), currentBlockPos, currentSide, providerClass));

                            synchronized (writeLock) {
                                currentUI = provider.provide(new SpatialUIContextClient(tileEntity, currentSide));
                            }

                            break;
                        }
                    }
                }
            } else if (System.currentTimeMillis() - lastValidTime > SWITCH_DELAY) {
                close();
            }

            synchronized (writeLock) {
                if (currentUI != null) {
                    currentUI.update();
                }
            }
        }
    }

    @SubscribeEvent
    public void handleWorldUnload(final WorldEvent.Unload event) {
        close();
    }

    // --------------------------------------------------------------------- //

    private static void setupMatrix(final EnumFacing side, final EnumFacing playerFacing) {
        switch (side) {
            case DOWN:
                GlStateManager.rotate(-90, 1, 0, 0);
                switch (playerFacing) {
                    case NORTH:
                        GlStateManager.rotate(180, 0, 0, 1);
                        break;
                    case WEST:
                        GlStateManager.rotate(-90, 0, 0, 1);
                        break;
                    case EAST:
                        GlStateManager.rotate(90, 0, 0, 1);
                        break;
                }
                break;
            case UP:
                GlStateManager.rotate(90, 1, 0, 0);
                switch (playerFacing) {
                    case NORTH:
                        GlStateManager.rotate(180, 0, 0, 1);
                        break;
                    case WEST:
                        GlStateManager.rotate(90, 0, 0, 1);
                        break;
                    case EAST:
                        GlStateManager.rotate(-90, 0, 0, 1);
                        break;
                }
                break;
            case NORTH:
                GlStateManager.rotate(0, 0, 1, 0);
                break;
            case SOUTH:
                GlStateManager.rotate(180, 0, 1, 0);
                break;
            case WEST:
                GlStateManager.rotate(90, 0, 1, 0);
                break;
            case EAST:
                GlStateManager.rotate(-90, 0, 1, 0);
                break;
        }

        GlStateManager.translate(0.5, 0.5, -0.505);
        GlStateManager.scale(-1, -1, 1);
    }

    private void handleClick(final PlayerInteractEvent event, final Consumer<PlayerInteractEvent> clickHandler) {
        if (event.getSide() == Side.SERVER) {
            if (event.getEntityPlayer() instanceof EntityPlayerMP) {
                final NetHandlerPlayServer client = ((EntityPlayerMP) event.getEntityPlayer()).connection;
                if (SpatialUIManagerServer.INSTANCE.isSubscribed(client)) {
                    event.setCanceled(true);
                }
            }
        } else {
            synchronized (writeLock) {
                if (currentUI != null) {
                    event.setCanceled(true);
                    clickHandler.accept(event);
                }
            }
        }
    }

    private void handleLeftClickClient(final PlayerInteractEvent event) {
        if (event.getHand() == EnumHand.OFF_HAND) {
            return;
        }

        final PlayerInteractEvent.LeftClickBlock click = (PlayerInteractEvent.LeftClickBlock) event;
        final Vector2 mousePosition = map(click.getHitVec(), click.getPos(), click.getEntityPlayer().getHorizontalFacing());
        currentUI.handleInput(InputEvent.getMouseEvent(InputEvent.Phase.BEGIN, 0, mousePosition));
        currentUI.handleInput(InputEvent.getMouseEvent(InputEvent.Phase.END, 0, mousePosition));
    }

    private void handleRightClickClient(final PlayerInteractEvent event) {
        if (event.getHand() == EnumHand.OFF_HAND) {
            return;
        }

        final PlayerInteractEvent.RightClickBlock click = (PlayerInteractEvent.RightClickBlock) event;
        final Vector2 mousePosition = map(click.getHitVec(), click.getPos(), click.getEntityPlayer().getHorizontalFacing());
        currentUI.handleInput(InputEvent.getMouseEvent(InputEvent.Phase.BEGIN, 1, mousePosition));
        currentUI.handleInput(InputEvent.getMouseEvent(InputEvent.Phase.END, 1, mousePosition));
    }

    private Vector2 map(final Vec3d hitVec, final BlockPos pos, final EnumFacing playerFacing) {
        final Vector2 local;
        switch (currentSide) {
            case DOWN: {
                final Vector2 south = new Vector2(1 - ((float) hitVec.xCoord - pos.getX()), (float) hitVec.zCoord - pos.getZ());
                switch (playerFacing) {
                    case NORTH:
                        local = new Vector2(1 - south.x, 1 - south.y); // 180
                        break;
                    case WEST:
                        //noinspection SuspiciousNameCombination This is fine.
                        local = new Vector2(1 - south.y, south.x); // 90
                        break;
                    case EAST:
                        //noinspection SuspiciousNameCombination This is fine.
                        local = new Vector2(south.y, 1 - south.x); // -90
                        break;
                    default:
                        local = south;
                }
                break;
            }
            case UP: {
                final Vector2 south = new Vector2(1 - ((float) hitVec.xCoord - pos.getX()), 1 - ((float) hitVec.zCoord - pos.getZ()));
                switch (playerFacing) {
                    case NORTH:
                        local = new Vector2(1 - south.x, 1 - south.y); // 180
                        break;
                    case WEST:
                        //noinspection SuspiciousNameCombination This is fine.
                        local = new Vector2(south.y, 1 - south.x); // -90
                        break;
                    case EAST:
                        //noinspection SuspiciousNameCombination This is fine.
                        local = new Vector2(1 - south.y, south.x); // 90
                        break;
                    default:
                        local = south;
                }
                break;
            }
            case NORTH:
                local = new Vector2(1 - ((float) hitVec.xCoord - pos.getX()), 1 - ((float) hitVec.yCoord - pos.getY()));
                break;
            case SOUTH:
                local = new Vector2((float) hitVec.xCoord - pos.getX(), 1 - ((float) hitVec.yCoord - pos.getY()));
                break;
            case WEST:
                local = new Vector2((float) hitVec.zCoord - pos.getZ(), 1 - ((float) hitVec.yCoord - pos.getY()));
                break;
            case EAST:
                local = new Vector2(1 - ((float) hitVec.zCoord - pos.getZ()), 1 - ((float) hitVec.yCoord - pos.getY()));
                break;
            default:
                throw new IllegalStateException();
        }

        return local;
    }

    // --------------------------------------------------------------------- //

    private SpatialUIManagerClient() {
    }
}
