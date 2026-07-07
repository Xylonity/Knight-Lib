package dev.xylonity.knightlib.client.screen.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Camera editor additional params and color specs
 */
public final class CameraEditorWidgets {

    public static final int ACCENT = 0xFFE8C04A;

    private static final int TEXT = 0xFFE2E6EE;
    private static final int TEXT_FAINT = 0xFF565E6E;

    private static final int FILL = 0xE8161923;
    private static final int FILL_HOVER = 0xF0232837;
    private static final int BORDER = 0xFF3A4152;
    private static final int BORDER_HOVER = 0xFF57617A;

    private static final int PRIMARY_FILL = 0xE82B2312;
    private static final int PRIMARY_FILL_HOVER = 0xF03D311A;
    private static final int PRIMARY_BORDER = 0xFF8A742F;
    private static final int PRIMARY_BORDER_HOVER = 0xFFC7A83E;
    private static final int PRIMARY_TEXT = 0xFFF0D178;

    private static final int DANGER_FILL = 0xE8231318;
    private static final int DANGER_FILL_HOVER = 0xF0361B23;
    private static final int DANGER_BORDER = 0xFF5C2B34;
    private static final int DANGER_BORDER_HOVER = 0xFF95434F;
    private static final int DANGER_TEXT = 0xFFFF7A85;

    private CameraEditorWidgets() {
        ;;
    }

    private static int fillFor(Style style, boolean hover) {
        return switch (style) {
            case NORMAL -> hover ? FILL_HOVER : FILL;
            case PRIMARY -> hover ? PRIMARY_FILL_HOVER : PRIMARY_FILL;
            case DANGER -> hover ? DANGER_FILL_HOVER : DANGER_FILL;
        };

    }

    private static int borderFor(Style style, boolean hover) {
        return switch (style) {
            case NORMAL -> hover ? BORDER_HOVER : BORDER;
            case PRIMARY -> hover ? PRIMARY_BORDER_HOVER : PRIMARY_BORDER;
            case DANGER -> hover ? DANGER_BORDER_HOVER : DANGER_BORDER;
        };

    }

    private static int textFor(Style style, boolean active) {
        if (!active) {
            return TEXT_FAINT;
        }

        return switch (style) {
            case NORMAL -> TEXT;
            case PRIMARY -> PRIMARY_TEXT;
            case DANGER -> DANGER_TEXT;
        };

    }

    private static void drawBox(GuiGraphics graphics, int x, int y, int width, int height, int fill, int border) {
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.fill(x, y, x + width, y + 1, border);
        graphics.fill(x, y + height - 1, x + width, y + height, border);
        graphics.fill(x, y, x + 1, y + height, border);
        graphics.fill(x + width - 1, y, x + width, y + height, border);
    }

    /**
     * A flat push button
     */
    public static class FlatButton extends AbstractButton {

        private final Style style;
        private final Runnable onPress;

        public FlatButton(int x, int y, int width, int height, Component message, Style style, Runnable onPress) {
            super(x, y, width, height, message);

            this.style = style;
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            onPress.run();
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            final boolean hover = active && isHoveredOrFocused();

            drawBox(graphics, getX(), getY(), width, height, fillFor(style, hover), borderFor(style, hover));

            final Font font = Minecraft.getInstance().font;
            graphics.drawString(font, getMessage(), getX() + (width - font.width(getMessage())) / 2, getY() + (height - 8) / 2, textFor(style, active), false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

    }

    /**
     * An on/off pill with a state dot
     */
    public static class FlatToggle extends AbstractButton {

        private final Consumer<Boolean> onChange;
        private boolean value;

        public FlatToggle(int x, int y, int width, int height, Component label, boolean initial, Consumer<Boolean> onChange) {
            super(x, y, width, height, label);

            this.value = initial;
            this.onChange = onChange;
        }

        public static int widthFor(Font font, String label) {
            return font.width(label) + 22;
        }

        @Override
        public void onPress() {
            value = !value;
            onChange.accept(value);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            final boolean hover = active && isHoveredOrFocused();
            final Style style = value ? Style.PRIMARY : Style.NORMAL;

            drawBox(graphics, getX(), getY(), width, height, fillFor(style, hover), borderFor(style, hover));

            final int dotY = getY() + height / 2 - 2;
            graphics.fill(getX() + 5, dotY, getX() + 9, dotY + 4, !active ? TEXT_FAINT : (value ? ACCENT : TEXT_FAINT));

            final Font font = Minecraft.getInstance().font;
            graphics.drawString(font, getMessage(), getX() + 13, getY() + (height - 8) / 2, textFor(style, active), false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

    }

    /**
     * Integer tick slider with a label
     */
    public static class FlatSlider extends AbstractSliderButton {

        private final String label;
        private final String suffix;
        private final int min;
        private final int max;
        private final IntConsumer onChange;

        public FlatSlider(int x, int y, int width, int height, String label, int min, int max, int initial, IntConsumer onChange, String suffix) {
            super(x, y, width, height, Component.empty(), (Mth.clamp(initial, min, max) - min) / (double) (max - min));

            this.label = label;
            this.suffix = suffix;
            this.min = min;
            this.max = max;
            this.onChange = onChange;

            updateMessage();
        }

        private int currentTicks() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label + ": " + currentTicks() + "t" + suffix));
        }

        @Override
        protected void applyValue() {
            onChange.accept(currentTicks());
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            final boolean hover = active && isHoveredOrFocused();

            drawBox(graphics, getX(), getY(), width, height, fillFor(Style.NORMAL, hover), borderFor(Style.NORMAL, hover));

            final int travel = width - 4;
            final int fillWidth = (int) Math.round(value * travel);

            if (fillWidth > 0) {
                graphics.fill(getX() + 2, getY() + height - 4, getX() + 2 + fillWidth, getY() + height - 2, 0x66E8C04A);
            }

            final int handleX = Mth.clamp(getX() + 2 + fillWidth, getX() + 2, getX() + width - 3);
            graphics.fill(handleX, getY() + 2, handleX + 1, getY() + height - 2, hover ? ACCENT : 0xFFB9C0CF);

            final Font font = Minecraft.getInstance().font;
            graphics.drawString(font, getMessage(), getX() + (width - font.width(getMessage())) / 2, getY() + (height - 8) / 2, textFor(Style.NORMAL, active), false);
        }

    }

    public enum Style {
        NORMAL,
        PRIMARY,
        DANGER
    }

}