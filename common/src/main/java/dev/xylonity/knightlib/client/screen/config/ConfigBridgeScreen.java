package dev.xylonity.knightlib.client.screen.config;

import dev.xylonity.knightlib.api.config.AutoConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

import static dev.xylonity.knightlib.client.screen.config.KnightLibConfigScreen.*;

/**
 * Shown when a mod has registered multiple {@link AutoConfig} classes.
 * Lists them in a short column, each entry opening the corresponding config editor.
 */
public class ConfigBridgeScreen extends Screen {

    private final List<ConfigCard> cards = new ArrayList<>();

    private final Screen parent;
    private final String modId;
    private final int accent;
    private final long openedAt = Util.getMillis();

    private static final int DEFAULT_ACCENT = 0xFF4A9EFF;
    private static final int CARD_HEIGHT = 32;
    private static final int CARD_GAP = 3;
    private static final int TITLE_SPACE = 30;
    private static final int BACK_GAP = 14;
    private static final int BACK_WIDTH = 80;
    private static final int BACK_HEIGHT = 18;

    private int listX;
    private int listY;
    private int listWidth;

    private final Minecraft minecraft = Minecraft.getInstance();

    public ConfigBridgeScreen(Screen parent, String modId, List<Class<?>> configs) {
        super(Component.translatable("knightlib.config.bridge.title", KnightLibConfigScreen.prettify(modId)));

        this.parent = parent;
        this.modId = modId;

        int foundAccent = DEFAULT_ACCENT;
        for (Class<?> clazz : configs) {
            AutoConfig meta = clazz.getAnnotation(AutoConfig.class);
            if (meta != null && meta.accentColor() != DEFAULT_ACCENT) {
                foundAccent = meta.accentColor();
                break;
            }

        }
        this.accent = foundAccent;

        for (Class<?> clazz : configs) {
            AutoConfig meta = clazz.getAnnotation(AutoConfig.class);
            if (meta == null) {
                continue;
            }

            String title = meta.title().isEmpty() ? KnightLibConfigScreen.prettify(meta.file()) : meta.title();
            String description = meta.description().isEmpty() ? meta.file() + ".toml" : meta.description();
            cards.add(new ConfigCard(clazz, title, description));
        }

    }

    @Override
    protected void init() {
        resetFrameClock();

        final int cardsHeight = cards.size() * CARD_HEIGHT + Math.max(0, cards.size() - 1) * CARD_GAP;
        final int blockHeight = TITLE_SPACE + cardsHeight + BACK_GAP + BACK_HEIGHT;

        this.listWidth = Math.min(300, this.width - 48);
        this.listX = (this.width - listWidth) / 2;
        this.listY = Math.max(12, (this.height - blockHeight) / 2) + TITLE_SPACE;

        this.addRenderableWidget(new KnightLibConfigScreen.FlatButton(
                (this.width - BACK_WIDTH) / 2, listY + cardsHeight + BACK_GAP, BACK_WIDTH, BACK_HEIGHT,
                Component.translatable("knightlib.config.bridge.back"),
                button -> this.minecraft.setScreen(this.parent), accent
        ).withEntranceDelay(120 + cards.size() * 45));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ;;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        advanceFrame();

        graphics.fill(0, 0, this.width, this.height, opaque(BACKGROUND));

        final Font font = minecraft.font;
        final long elapsed = Util.getMillis() - openedAt;
        final int textX = listX + 10;
        final int textWidth = listWidth - 20;

        drawRising(graphics, font, ellipsize(font, KnightLibConfigScreen.prettify(modId), Style.EMPTY.withBold(true), textWidth), textX, listY - 26, 0xFFFFFF, ease(elapsed / 380f));
        drawRising(graphics, font, ellipsize(font, I18n.get("knightlib.config.bridge.subtitle"), textWidth), textX, listY - 14, TEXT_MUTED, ease((elapsed - 70) / 380f));

        for (int i = 0; i < cards.size(); i++) {
            renderCard(graphics, font, cards.get(i), i, listX, cardY(i), listWidth, mouseX, mouseY, elapsed);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int cardY(int index) {
        return listY + index * (CARD_HEIGHT + CARD_GAP);
    }

    private void renderCard(GuiGraphics graphics, Font font, ConfigCard card, int index, int x, int y, int width, int mouseX, int mouseY, long elapsed) {
        final boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + CARD_HEIGHT;
        card.hoverAnim = approach(card.hoverAnim, hovered ? 1f : 0f, hovered ? 18f : 8f);
        final float e = smooth(card.hoverAnim);

        final float appear = ease((elapsed - 60 - index * 45L) / 340f);
        if (appear < 0.02f) {
            return;
        }

        beginFade(graphics, appear);
        graphics.pose().pushPose();
        graphics.pose().translate(0f, (1f - appear) * 6f, 0f);

        frame(graphics, x, y, x + width, y + CARD_HEIGHT, opaque(mixRgb(SURFACE, ROW_HOVER, e)), opaque(mixRgb(SURFACE_BORDER, FIELD_BORDER, e)), 2);

        final int textMaxWidth = width - 20 - ICON_CHEVRON_RIGHT.width - 8;
        final int blockY = y + (CARD_HEIGHT - (font.lineHeight * 2 + 1)) / 2 + 1;

        graphics.drawString(font, ellipsize(font, card.title, textMaxWidth), x + 10, blockY, opaque(mixRgb(TEXT_LABEL, 0xFFFFFF, e)), false);
        graphics.drawString(font, ellipsize(font, card.description, textMaxWidth), x + 10, blockY + font.lineHeight + 1, opaque(mixRgb(TEXT_MUTED, TEXT_DIM, e)), false);

        graphics.pose().pushPose();

        graphics.pose().translate(e * 2f, 0f, 0f);
        ICON_CHEVRON_RIGHT.draw(graphics, x + width - 10 - ICON_CHEVRON_RIGHT.width, y + (CARD_HEIGHT - ICON_CHEVRON_RIGHT.height) / 2, opaque(mixRgb(0x46464C, accent, e)));

        graphics.pose().popPose();

        graphics.pose().popPose();
        endFade(graphics, appear);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        for (int i = 0; i < cards.size(); i++) {
            final int y = cardY(i);
            if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= y && mouseY < y + CARD_HEIGHT) {
                KnightLibConfigScreen.playClickSound();
                this.minecraft.setScreen(new KnightLibConfigScreen(this, cards.get(i).configClass));
                return true;
            }

        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static class ConfigCard {

        final Class<?> configClass;
        final String title;
        final String description;
        float hoverAnim = 0f;

        ConfigCard(Class<?> configClass, String title, String description) {
            this.configClass = configClass;
            this.title = title;
            this.description = description;
        }

    }

}
