package li.cil.circuity.common.item;

import li.cil.circuity.api.bus.BusController;
import li.cil.circuity.api.bus.BusElement;
import li.cil.circuity.api.bus.controller.AddressMapper;
import li.cil.circuity.api.bus.controller.InterruptMapper;
import li.cil.circuity.api.bus.device.AddressBlock;
import li.cil.circuity.api.bus.device.Addressable;
import li.cil.circuity.api.bus.device.InterruptSink;
import li.cil.circuity.api.bus.device.InterruptSource;
import li.cil.circuity.client.gui.GuiType;
import li.cil.circuity.common.Constants;
import li.cil.circuity.common.capabilities.CapabilityBusElement;
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
        SELECT_ADDRESS_MAPPING,
        SELECT_ADDRESS,
        APPLY_ADDRESS,
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
        final BusElement element = CapabilityUtil.getCapability(world, pos, side, CapabilityBusElement.BUS_ELEMENT_CAPABILITY, BusElement.class);
        if (element == null) {
            if (player.isSneaking()) {
                cycleMode(stack, world, player);
                return EnumActionResult.SUCCESS;
            }
            return super.onItemUse(stack, player, world, pos, hand, side, hitX, hitY, hitZ);
        }

        final BusController controller = element.getBusController();
        if (world.isRemote || controller == null) {
            return EnumActionResult.SUCCESS;
        }

        final NBTTagCompound tag = ItemUtil.getOrAddTagCompound(stack);
        final Mode mode = Mode.readFromNBT(tag);
        switch (mode) {
            case SELECT_ADDRESS: {
                if (element instanceof Addressable) {
                    final Addressable addressable = (Addressable) element;
                    final AddressMapper mapper = controller.getSubsystem(AddressMapper.class);
                    final AddressBlock memory = mapper.getAddressBlock(addressable);
                    tag.setLong(ADDRESS_TAG, memory.getOffset());

                    PlayerUtil.openGui(player, GuiType.SELECT_ADDRESS, memory.getOffset());
                }
                break;
            }
            case APPLY_ADDRESS: {
                if (element instanceof Addressable) {
                    final Addressable addressable = (Addressable) element;
                    final AddressMapper mapper = controller.getSubsystem(AddressMapper.class);
                    final AddressBlock memory = mapper.getAddressBlock(addressable);
                    mapper.setDeviceAddress(addressable, memory.at(tag.getLong(ADDRESS_TAG)));
                    player.addChatMessage(new TextComponentTranslation(Constants.I18N.CONFIGURATOR_ADDRESS_APPLIED, String.format("%04X", memory.getOffset())));
                }
                break;
            }
            case SELECT_INTERRUPT_SOURCE: {
                if (element instanceof InterruptSource) {
                    final InterruptSource source = (InterruptSource) element;
                    final InterruptMapper mapper = controller.getSubsystem(InterruptMapper.class);
                    final PrimitiveIterator.OfInt ids = mapper.getInterruptSourceIds(source);
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
                if (element instanceof InterruptSink) {
                    final InterruptSink sink = (InterruptSink) element;
                    final InterruptMapper mapper = controller.getSubsystem(InterruptMapper.class);
                    final PrimitiveIterator.OfInt ids = mapper.getInterruptSinkIds(sink);
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
                if (element instanceof BusController) {
                    final int sourceId = tag.getInteger(INTERRUPT_SOURCE_TAG);
                    final int sinkId = tag.getInteger(INTERRUPT_SINK_TAG);

                    if (sourceId < 0) {
                        return EnumActionResult.SUCCESS;
                    }

                    final InterruptMapper mapper = controller.getSubsystem(InterruptMapper.class);
                    mapper.setInterruptMapping(sourceId, sinkId);

                    if (sinkId >= 0) {
                        player.addChatMessage(new TextComponentTranslation(Constants.I18N.CONFIGURATOR_INTERRUPT_SET));
                    } else {
                        player.addChatMessage(new TextComponentTranslation(Constants.I18N.CONFIGURATOR_INTERRUPT_CLEARED));
                    }
                }
                break;
            }
            case SELECT_ADDRESS_MAPPING: {
                if (element instanceof BusController) {
                    final AddressMapper mapper = controller.getSubsystem(AddressMapper.class);
                    final int count = mapper.getConfigurationCount();
                    mapper.setActiveConfiguration((mapper.getActiveConfiguration() + 1) % count);
                    player.addChatComponentMessage(new TextComponentTranslation(Constants.I18N.CONFIGURATOR_MAPPING_SELECTED, count));
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
