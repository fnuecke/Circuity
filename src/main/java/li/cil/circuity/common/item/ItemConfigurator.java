package li.cil.circuity.common.item;

import li.cil.circuity.api.bus.BusDevice;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.circuity.api.bus.device.InterruptSource;
import li.cil.circuity.common.Constants;
import li.cil.circuity.common.capabilities.CapabilityBusDevice;
import li.cil.lib.util.CapabilityUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class ItemConfigurator extends Item {
    private static final String MODE_TAG = "mode";

    public enum Mode {
        CHANGE_ADDRESS,
        BIND_ADDRESS,
        BIND_INTERRUPT;

        private static final Mode[] VALUES = Mode.values();

        public Mode next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }

        public static Mode readFromItemStack(final ItemStack stack) {
            final NBTTagCompound tag = getTagCompound(stack);
            final int ordinal = tag.getInteger(MODE_TAG);
            final int clamped = Math.max(0, Math.min(ordinal, VALUES.length - 1));
            return VALUES[clamped];
        }

        public void writeToItemStack(final ItemStack stack) {
            final NBTTagCompound tag = getTagCompound(stack);
            tag.setInteger(MODE_TAG, ordinal());
        }

        private static NBTTagCompound getTagCompound(final ItemStack stack) {
            final NBTTagCompound tag;
            if (!stack.hasTagCompound()) {
                stack.setTagCompound(tag = new NBTTagCompound());
            } else {
                tag = stack.getTagCompound();
                assert tag != null : "ItemStack#hasTackCompound lied to me!";
            }
            return tag;
        }

        public String getLocalizationId() {
            return Constants.CONFIGURATOR_NAME + ".mode." + this.toString().toLowerCase();
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(final ItemStack stack, final World world, final EntityPlayer player, final EnumHand hand) {
        final Mode oldMode = Mode.readFromItemStack(stack);
        final Mode newMode = oldMode.next();
        newMode.writeToItemStack(stack);

        if (!world.isRemote) {
            player.addChatMessage(new TextComponentTranslation(Constants.I18N.CONFIGURATOR_MODE_CHANGED, new TextComponentTranslation(newMode.getLocalizationId())));
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public EnumActionResult onItemUse(final ItemStack stack, final EntityPlayer player, final World world, final BlockPos pos, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        final BusDevice device = CapabilityUtil.getCapability(world, pos, side, CapabilityBusDevice.BUS_DEVICE_CAPABILITY, BusDevice.class);
        final Mode mode = Mode.readFromItemStack(stack);
        if (device != null) {
            switch (mode) {
                case CHANGE_ADDRESS: {
                    if (device instanceof Addressable) {
                        return EnumActionResult.SUCCESS;
                    }
                    break;
                }
                case BIND_ADDRESS: {
                    if (player.isSneaking()) {
                        if (device instanceof Addressable) {
                            return EnumActionResult.SUCCESS;
                        }
                    } else {
                        if (device instanceof Object) {
                            // TODO Custom interface for setting target addresses on devices.
                            return EnumActionResult.SUCCESS;
                        }
                    }
                    break;
                }
                case BIND_INTERRUPT: {
                    if (player.isSneaking()) {
                        if (device instanceof InterruptSink) {
                            return EnumActionResult.SUCCESS;
                        }
                    } else {
                        if (device instanceof InterruptSource) {
                            return EnumActionResult.SUCCESS;
                        }
                    }
                    break;
                }
            }
            return EnumActionResult.FAIL;
        }
        return super.onItemUse(stack, player, world, pos, hand, side, hitX, hitY, hitZ);
    }
}
