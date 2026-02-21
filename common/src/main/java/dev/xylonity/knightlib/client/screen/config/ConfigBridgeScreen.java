package dev.xylonity.knightlib.client.screen.config;

import dev.xylonity.knightlib.api.config.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Shown when a mod has registered multiple {@link AutoConfig} classes.
 * Presents a list of compact cards that the player can click to open the
 * corresponding config editor.
 */
public class ConfigBridgeScreen extends Screen {

    private final List<ConfigCard> cards = new ArrayList<>();

    private final Screen parent;
    private final String modId;
    private final int accent;

    private static final int DEFAULT_ACCENT = 0xFF4A9EFF;

    private final Minecraft minecraft = Minecraft.getInstance();

    public ConfigBridgeScreen(Screen parent, String modId, List<Class<?>> configs) {
        super(Component.literal(KnightLibConfigScreen.prettify(modId) + " — Configurations"));

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
        final int backButtonWidth = 80;
        final int backButtonHeight = 18;
        final int backButtonX = (this.width - backButtonWidth) / 2;

        final int cardHeight = 32;
        final int gap = 4;
        final int backButtonGap = 14;
        final int totalCardsH = cards.size() * cardHeight + (cards.size() - 1) * gap;
        final int totalBlockH = totalCardsH + backButtonGap + backButtonHeight;
        final int startY = Math.max(56, (this.height - totalBlockH) / 2);
        final int backButtonY = startY + totalCardsH + backButtonGap;

        KnightLibConfigScreen.FlatButton backButton = new KnightLibConfigScreen.FlatButton(
                backButtonX, backButtonY, backButtonWidth, backButtonHeight,
                Component.literal("\u2190 Back"),
                button -> this.minecraft.setScreen(this.parent), accent
        );

        this.addRenderableWidget(backButton);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF0E0E0E);

        // Header gradient
        for (int i = 0; i < 48; i++) {
            int alpha = Math.max(0, 50 - i * 2);
            if (alpha > 0) {
                graphics.fill(0, i, this.width, i + 1, alpha << 24);
            }

        }

        final String headerTitle = KnightLibConfigScreen.prettify(modId);
        graphics.drawString(minecraft.font, headerTitle, this.width - 14 - minecraft.font.width(headerTitle), 10, 0xFFFFFFFF, false);

        final String subtitle = "Select a configuration to edit";
        graphics.drawString(minecraft.font, subtitle, this.width - 14 - minecraft.font.width(subtitle), 22, 0xFF888888, false);

        final int cardWidth = Math.min(280, this.width - 60);
        final int cardHeight = 32;
        final int gap = 4;
        final int backButtonHeight = 18;
        final int backButtonGap = 14;
        final int totalCardsHeight = cards.size() * cardHeight + (cards.size() - 1) * gap;
        final int totalBlockHeight = totalCardsHeight + backButtonGap + backButtonHeight;
        final int startY = Math.max(56, (this.height - totalBlockHeight) / 2);
        final int startX = (this.width - cardWidth) / 2;

        for (int i = 0; i < cards.size(); i++) {
            int y = startY + i * (cardHeight + gap);
            renderCard(graphics, cards.get(i), i, startX, y, cardWidth, cardHeight, mouseX, mouseY);
        }

        // Footer line between cards and back button
        int footerY = startY + totalCardsHeight + backButtonGap / 2;
        int lineW = (int) (cardWidth * 0.8f);
        int lineX = (this.width - lineW) / 2;
        KnightLibConfigScreen.drawFadedHLine(graphics, lineX, lineX + lineW, footerY, 0x4D4D4D);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderCard(GuiGraphics graphics, ConfigCard card, int index, int x, int y, int width, int height, int mouseX, int mouseY) {

        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        final float target = hovered ? 1f : 0f;
        final float diff = target - card.hoverAnim;
        card.hoverAnim += diff * 0.12f;
        if (Math.abs(diff) < 0.005f) {
            card.hoverAnim = target;
        }

        float e = card.hoverAnim * card.hoverAnim * (3f - 2f * card.hoverAnim);

        final int baseAlpha = (index % 2 == 0) ? 0x1A : 0x14;
        final int extraAlpha = (int) (0x20 * e);
        graphics.fill(x, y, x + width, y + height, (Math.min(255, baseAlpha + extraAlpha) << 24) | 0x1A1A1A);

        int[] ac = KnightLibConfigScreen.rgb(accent);
        int baseRed = (int) (0x25 + (ac[0] - 0x25) * e);
        int baseGreen = (int) (0x25 + (ac[1] - 0x25) * e);
        int baseBlue = (int) (0x25 + (ac[2] - 0x25) * e);
        graphics.fill(x, y, x + 2, y + height, 0xFF000000 | (baseRed << 16) | (baseGreen << 8) | baseBlue);

        if (e > 0.01f) {
            int eAlpha = (int) (0xFF * e);
            graphics.fill(x + 2, y, x + 3, y + height, (eAlpha << 24) | (ac[0] << 16) | (ac[1] << 8) | ac[2]);
            if (e > 0.5f) {
                int eAlpha2 = (int) (0xFF * (e - 0.5f) * 2f);
                graphics.fill(x + 3, y, x + 4, y + height, (eAlpha2 << 24) | (ac[0] << 16) | (ac[1] << 8) | ac[2]);
            }

        }

        KnightLibConfigScreen.drawBarGlow(graphics, x, y, height, e, accent);
        KnightLibConfigScreen.drawHoverParticles(graphics, x, y, height, e, accent, index * 73 + card.title.hashCode());

        if (e > 0.01f) {
            int gradWidth = Math.min(60, width / 4);
            int maxAlpha = (int) (0x18 * e);
            for (int gx = 0; gx < gradWidth; gx++) {
                float time = (float) gx / gradWidth;
                int alpha = (int) (maxAlpha * (1f - time * time));
                if (alpha > 0) {
                    graphics.fill(x + gx, y, x + gx + 1, y + height, (alpha << 24) | (ac[0] << 16) | (ac[1] << 8) | ac[2]);
                }

            }

        }

        final int titleHeight = minecraft.font.lineHeight;
        final int descriptionHeight = minecraft.font.lineHeight;
        final int innerGap = 1;
        final int blockHeight = titleHeight + innerGap + descriptionHeight;
        final int blockY = y + (height - blockHeight) / 2;

        final int arrowWidth = minecraft.font.width("\u2192") + 6;
        final int textMaxWidth = width - 10 - arrowWidth;

        String titleString = card.title;
        if (minecraft.font.width(titleString) > textMaxWidth) {
            titleString = minecraft.font.plainSubstrByWidth(titleString, textMaxWidth - minecraft.font.width("...")) + "...";
        }

        graphics.drawString(minecraft.font, titleString, x + 10, blockY, 0xFFE8E8E8, false);

        String descriptionString = card.description;
        if (minecraft.font.width(descriptionString) > textMaxWidth) {
            descriptionString = minecraft.font.plainSubstrByWidth(descriptionString, textMaxWidth - minecraft.font.width("...")) + "...";
        }

        graphics.drawString(minecraft.font, descriptionString, x + 10, blockY + titleHeight + innerGap, 0xFF606060, false);

        int[] arr = KnightLibConfigScreen.rgb(accent);
        final int aRed = (int) (0x44 + (arr[0] - 0x44) * e);
        final int aGreen = (int) (0x44 + (arr[1] - 0x44) * e);
        final int aBlue = (int) (0x44 + (arr[2] - 0x44) * e);
        graphics.drawString(minecraft.font, "\u2192", x + width - arrowWidth + 2, y + (height - minecraft.font.lineHeight) / 2, 0xFF000000 | (aRed << 16) | (aGreen << 8) | aBlue, false);

        graphics.fill(x + 4, y, x + width - 4, y + 1, 0x08FFFFFF);
        graphics.fill(x + 4, y + height - 1, x + width - 4, y + height, 0x08FFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        final int cardWidth = Math.min(280, this.width - 60);
        final int cardHeight = 32;
        final int gap = 4;
        final int backButtonHeight = 18;
        final int backButtonGap = 14;
        final int totalCardsHeight = cards.size() * cardHeight + (cards.size() - 1) * gap;
        final int totalBlockHeight = totalCardsHeight + backButtonGap + backButtonHeight;
        final int startY = Math.max(56, (this.height - totalBlockHeight) / 2);
        final int startX = (this.width - cardWidth) / 2;

        for (int i = 0; i < cards.size(); i++) {
            int y = startY + i * (cardHeight + gap);
            if (mouseX >= startX && mouseX <= startX + cardWidth && mouseY >= y && mouseY <= y + cardHeight) {
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