package li.cil.circuity.common.item;

import li.cil.circuity.common.Constants;
import li.cil.circuity.common.init.Items;
import li.cil.lib.util.ItemUtil;
import li.cil.lib.util.PlayerUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class ItemConfigurator extends Item {
    private static final String MODE_TAG = "mode";
    private static final String INTERRUPT_SOURCE_TAG = "source";

    public enum Mode {
        ADDRESS_MAPPING,
        ADDRESS,
        INTERRUPT;

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

    public static boolean isMode(final EntityPlayer player, final Mode mode) {
        return getMode(player) == mode;
    }

    public static boolean isMode(final ItemStack stack, final Mode mode) {
        return getMode(stack) == mode;
    }

    @Nullable
    public static Mode getMode(final EntityPlayer player) {
        final ItemStack stack = PlayerUtil.getHeldItemOfType(player, Items.configurator);
        return stack.isEmpty() ? null : Mode.readFromNBT(ItemUtil.getOrAddTagCompound(stack));
    }

    @Nullable
    public static Mode getMode(final ItemStack stack) {
        if (!stack.isItemEqual(new ItemStack(Items.configurator))) {
            return null;
        }
        return Mode.readFromNBT(ItemUtil.getOrAddTagCompound(stack));
    }

    public static void setInterruptSource(final ItemStack stack, final int sourceId) {
        ItemUtil.getOrAddTagCompound(stack).setInteger(INTERRUPT_SOURCE_TAG, sourceId);
    }

    public static int getInterruptSource(final ItemStack stack) {
        return ItemUtil.getOrAddTagCompound(stack).getInteger(INTERRUPT_SOURCE_TAG);
    }

    public static boolean hasInterruptSource(final ItemStack stack) {
        return ItemUtil.getOrAddTagCompound(stack).hasKey(INTERRUPT_SOURCE_TAG, net.minecraftforge.common.util.Constants.NBT.TAG_INT);
    }

    public static void clearInterruptSource(final ItemStack stack) {
        ItemUtil.getOrAddTagCompound(stack).removeTag(INTERRUPT_SOURCE_TAG);
    }

    @Override
    public boolean doesSneakBypassUse(final ItemStack stack, final IBlockAccess world, final BlockPos pos, final EntityPlayer player) {
        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(final World world, final EntityPlayer player, final EnumHand hand) {
        final ItemStack stack = player.getHeldItem(hand);
        final NBTTagCompound tag = ItemUtil.getOrAddTagCompound(stack);

        if (player.isSneaking()) {
            tag.removeTag(INTERRUPT_SOURCE_TAG);

            if (world.isRemote) {
                PlayerUtil.addLocalChatMessage(player, new TextComponentTranslation(Constants.I18N.CONFIGURATOR_CLEARED));
            }
        } else {
            cycleMode(stack, world, player);
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
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
}
