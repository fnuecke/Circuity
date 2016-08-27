package li.cil.circuity.common.item;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.ConfigurableBusController;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.circuity.api.bus.device.InterruptSource;
import li.cil.circuity.common.Constants;
import li.cil.circuity.common.capabilities.CapabilityBusDevice;
import li.cil.lib.util.CapabilityUtil;
import li.cil.lib.util.ItemUtil;
import li.cil.lib.util.PlayerUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.PrimitiveIterator;

public class ItemConfigurator extends Item {
    private static final String MODE_TAG = "mode";
    private static final String ADDRESS_TAG = "address";
    private static final String INTERRUPT_SOURCE_TAG = "source";
    private static final String INTERRUPT_SINK_TAG = "sink";

    public enum Mode {
        CHANGE_ADDRESS,
        SELECT_ADDRESS,
        BIND_ADDRESS,
        SELECT_INTERRUPT_SOURCE,
        SELECT_INTERRUPT_SINK,
        BIND_INTERRUPT;

        private static final Mode[] VALUES = Mode.values();

        public Mode next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }

        public static Mode readFromNBT(final NBTTagCompound tag) {
            final int ordinal = tag.getInteger(MODE_TAG);
            final int clamped = Math.max(0, Math.min(ordinal, VALUES.length - 1));
            return VALUES[clamped];
        }

        public void writeToNBT(final NBTTagCompound tag) {
            tag.setInteger(MODE_TAG, ordinal());
        }

        public String getLocalizationId() {
            return Constants.CONFIGURATOR_NAME + ".mode." + this.toString().toLowerCase();
        }
    }

    @Override
    public boolean doesSneakBypassUse(final ItemStack stack, final IBlockAccess world, final BlockPos pos, final EntityPlayer player) {
        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(final ItemStack stack, final World world, final EntityPlayer player, final EnumHand hand) {
        final NBTTagCompound tag = ItemUtil.getOrAddTagCompound(stack);
        final Mode oldMode = Mode.readFromNBT(tag);

        if (player.isSneaking()) {
            tag.removeTag(ADDRESS_TAG);
            tag.removeTag(INTERRUPT_SOURCE_TAG);
            tag.removeTag(INTERRUPT_SINK_TAG);

            if (world.isRemote) {
                PlayerUtil.addLocalChatMessage(player, new TextComponentTranslation(Constants.I18N.CONFIGURATOR_CLEARED));
            }
        } else {
            final Mode newMode = oldMode.next();
            newMode.writeToNBT(tag);

            if (world.isRemote) {
                PlayerUtil.addLocalChatMessage(player, new TextComponentTranslation(Constants.I18N.CONFIGURATOR_MODE_CHANGED, new TextComponentTranslation(newMode.getLocalizationId())));
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public EnumActionResult onItemUse(final ItemStack stack, final EntityPlayer player, final World world, final BlockPos pos, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        final BusDevice device = CapabilityUtil.getCapability(world, pos, side, CapabilityBusDevice.BUS_DEVICE_CAPABILITY, BusDevice.class);
        if (device == null) {
            if (player.isSneaking()) {
                cycleMode(stack, world, player);
                return EnumActionResult.SUCCESS;
            }
            return super.onItemUse(stack, player, world, pos, hand, side, hitX, hitY, hitZ);
        }

        final BusController controller = device.getBusController();
        if (world.isRemote || controller == null) {
            return EnumActionResult.SUCCESS;
        }

        final NBTTagCompound tag = ItemUtil.getOrAddTagCompound(stack);
        final Mode mode = Mode.readFromNBT(tag);
        switch (mode) {
            case CHANGE_ADDRESS: {
                if (device instanceof Addressable) {
                    final Addressable addressable = (Addressable) device;
                    // TODO Open GUI for address input, send it back to server, apply to device via controller.
                }
                break;
            }
            case SELECT_ADDRESS: {
                if (device instanceof Addressable) {
                    final Addressable addressable = (Addressable) device;
                    final AddressBlock memory = controller.getAddress(addressable);

                    if (memory == null) {
                        return EnumActionResult.SUCCESS;
                    }

                    tag.setLong(ADDRESS_TAG, memory.getOffset());

                    player.addChatMessage(new TextComponentTranslation(Constants.I18N.CONFIGURATOR_ADDRESS_SELECTED, String.format("%04X", memory.getOffset())));
                }
                break;
            }
            case BIND_ADDRESS: {
                if (device instanceof Object) {
                    // TODO Custom interface for setting target addresses on devices.
                }
                break;
            }
            case SELECT_INTERRUPT_SOURCE: {
                if (device instanceof InterruptSource) {
                    final InterruptSource source = (InterruptSource) device;
                    final PrimitiveIterator.OfInt ids = controller.getInterruptSourceIds(source);
                    final int oldId = tag.getInteger(INTERRUPT_SOURCE_TAG);
                    final int newId = getNextId(ids, oldId);
                    tag.setInteger(INTERRUPT_SOURCE_TAG, newId);

                    if (newId >= 0) {
                        final ITextComponent interruptName = source.getInterruptName(newId);
                        final ITextComponent displayName = interruptName != null ? interruptName : new TextComponentTranslation(Constants.I18N.UNKNOWN);
                        player.addChatMessage(new TextComponentTranslation(Constants.I18N.CONFIGURATOR_INTERRUPT_SOURCE, displayName, newId));
                    } else {
                        player.addChatMessage(new TextComponentTranslation(Constants.I18N.CONFIGURATOR_NO_INTERRUPT_SOURCE));
                    }
                }
                break;
            }
            case SELECT_INTERRUPT_SINK: {
                if (device instanceof InterruptSink) {
                    final InterruptSink sink = (InterruptSink) device;
                    final PrimitiveIterator.OfInt ids = controller.getInterruptSinkIds(sink);
                    final int oldId = tag.getInteger(INTERRUPT_SINK_TAG);
                    final int newId = getNextId(ids, oldId);
                    tag.setInteger(INTERRUPT_SINK_TAG, newId);

                    if (newId >= 0) {
                        final ITextComponent interruptName = sink.getInterruptName(newId);
                        final ITextComponent displayName = interruptName != null ? interruptName : new TextComponentTranslation(Constants.I18N.UNKNOWN);
                        player.addChatMessage(new TextComponentTranslation(Constants.I18N.CONFIGURATOR_INTERRUPT_SINK, displayName, newId));
                    } else {
                        player.addChatMessage(new TextComponentTranslation(Constants.I18N.CONFIGURATOR_NO_INTERRUPT_SINK));
                    }
                }
                break;
            }
            case BIND_INTERRUPT: {
                if (device instanceof ConfigurableBusController) {
                    final ConfigurableBusController configurableController = (ConfigurableBusController) device;
                    final int sourceId = tag.getInteger(INTERRUPT_SOURCE_TAG);
                    final int sinkId = tag.getInteger(INTERRUPT_SINK_TAG);

                    if (sourceId < 0) {
                        return EnumActionResult.SUCCESS;
                    }

                    configurableController.setInterruptMapping(sourceId, sinkId);

                    if (sinkId >= 0) {
                        player.addChatMessage(new TextComponentTranslation(Constants.I18N.CONFIGURATOR_INTERRUPT_SET));
                    } else {
                        player.addChatMessage(new TextComponentTranslation(Constants.I18N.CONFIGURATOR_INTERRUPT_CLEARED));
                    }
                }
                break;
            }
        }

        return EnumActionResult.SUCCESS;
    }

    private static void cycleMode(final ItemStack stack, final World world, final EntityPlayer player) {
        final NBTTagCompound tag = ItemUtil.getOrAddTagCompound(stack);
        final Mode oldMode = Mode.readFromNBT(tag);
        final Mode newMode = oldMode.next();
        newMode.writeToNBT(tag);

        if (world.isRemote) {
            PlayerUtil.addLocalChatMessage(player, new TextComponentTranslation(Constants.I18N.CONFIGURATOR_MODE_CHANGED, new TextComponentTranslation(newMode.getLocalizationId())));
        }
    }

    private static int getNextId(final PrimitiveIterator.OfInt ids, final int id) {
        if (ids.hasNext()) {
            final int firstId = ids.nextInt();
            while (firstId != id && ids.hasNext()) {
                if (ids.nextInt() == id)
                    break;
            }
            if (ids.hasNext()) {
                return ids.nextInt();
            } else {
                return firstId;
            }
        } else {
            return -1;
        }
    }
}
