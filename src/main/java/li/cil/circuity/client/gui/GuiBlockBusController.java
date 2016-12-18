package li.cil.circuity.client.gui;

import li.cil.circuity.common.Constants;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiBlockBusController extends GuiContainer {
    private enum ButtonId {
        OK,
        CANCEL
    }

    private enum ComponentId {
        ADDRESS
    }

    private static final int BUTTON_OK_WIDTH = 150;
    private static final int BUTTON_OK_HEIGHT = 20;
    private static final int BUTTON_CANCEL_WIDTH = 150;
    private static final int BUTTON_CANCEL_HEIGHT = 20;
    private static final int TEXT_FIELD_ADDRESS_WIDTH = 316;
    private static final int PADDING = 4;

    private long address;

    private GuiButton buttonOk;
    private GuiButton buttonCancel;
    private GuiTextField textFieldAddress;

    public GuiBlockBusController(final Container container, final long initialAddress) {
        super(container);
        address = initialAddress;
    }

    @Override
    public void initGui() {
        super.initGui();

        Keyboard.enableRepeatEvents(true);

        buttonList.clear();
        buttonOk = addButton(new GuiButton(ButtonId.OK.ordinal(), width / 2 - PADDING - BUTTON_OK_WIDTH, height - BUTTON_OK_HEIGHT - PADDING, BUTTON_OK_WIDTH, BUTTON_OK_HEIGHT, I18n.format(Constants.I18N.BUTTON_OK)));
        buttonCancel = addButton(new GuiButton(ButtonId.CANCEL.ordinal(), width / 2 + PADDING, height - BUTTON_CANCEL_HEIGHT - PADDING, BUTTON_CANCEL_WIDTH, BUTTON_CANCEL_HEIGHT, I18n.format(Constants.I18N.BUTTON_CANCEL)));

        textFieldAddress = new GuiTextField(ComponentId.ADDRESS.ordinal(), fontRendererObj, width / 2 - TEXT_FIELD_ADDRESS_WIDTH / 2, PADDING, width, height);

        buttonOk.enabled = false;
        buttonCancel.enabled = false;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(final float partialTicks, final int mouseX, final int mouseY) {

    }

    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }


    @Override
    public void updateScreen() {
        super.updateScreen();

        textFieldAddress.updateCursorCounter();
    }

    @Override
    protected void keyTyped(final char typedChar, final int keyCode) throws IOException {
        textFieldAddress.textboxKeyTyped(typedChar, keyCode);

        super.keyTyped(typedChar, keyCode);
    }
}
