package li.cil.lib.client.gui.widget;

import li.cil.lib.api.gui.input.InputEvent;
import li.cil.lib.api.gui.layout.Alignment;
import li.cil.lib.api.math.Rect;
import li.cil.lib.api.math.Vector2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class Button extends AbstractLabel<Button> implements AbstractEventHandler<Button> {
    private final Set<Consumer<Button>> listeners = new HashSet<>();

    // --------------------------------------------------------------------- //

    public Button() {
        setHorizontalAlignment(Alignment.Horizontal.CENTER);
        setVerticalAlignment(Alignment.Vertical.MIDDLE);
    }

    // --------------------------------------------------------------------- //
    // Layoutable

    @Override
    public int getMinHeight() {
        return super.getMinHeight() + getRenderSettings().getButtonPadding() * 2;
    }

    @Override
    public int getPreferredWidth() {
        return super.getPreferredWidth() + getRenderSettings().getButtonPadding() * 2;
    }

    @Override
    public int getPreferredHeight() {
        return getMinHeight() + getRenderSettings().getButtonPadding() * 2;
    }

    // --------------------------------------------------------------------- //
    // Widget

    @Override
    public void render() {
        drawElevatedQuad(0, 0, getWidth(), getHeight(), 1);

        super.render();

        GlStateManager.disableTexture2D();

        final Rect localBounds = new Rect(Vector2.ZERO, getSize());
        final Vector2 localMousePosition = toLocal(getInputSystem().getMousePosition());
        if (localBounds.contains(localMousePosition)) {
            GlStateManager.color(1f, 1f, 1f, 0.3f);
            drawQuad(0, 0, getWidth(), getHeight());
        }

        GlStateManager.enableTexture2D();
    }

    // --------------------------------------------------------------------- //
    // EventHandler

    @Override
    public boolean processInput(final InputEvent event) {
        if (event.getType() == InputEvent.Type.MOUSE && event.getPhase() == InputEvent.Phase.BEGIN) {
            Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1f));
            listeners.forEach(this::notifyListener);
            return true;
        }
        return false;
    }

    // --------------------------------------------------------------------- //

    public Button addListener(final Consumer<Button> listener) {
        listeners.add(listener);

        return this;
    }

    public void removeListener(final Consumer<Button> listener) {
        listeners.remove(listener);
    }

    // --------------------------------------------------------------------- //

    private void notifyListener(final Consumer<Button> consumer) {
        consumer.accept(this);
    }
}
