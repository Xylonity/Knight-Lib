package dev.xylonity.knightlib.client.screen.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.util.KnightLibEasings;
import dev.xylonity.knightlib.config.interop.ConfigManager;
import dev.xylonity.knightlib.api.config.AutoConfig;
import dev.xylonity.knightlib.api.config.ConfigEntry;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class KnightLibConfigScreen extends Screen {

    private final Screen parent;
    private final Class<?> configClass;
    private final AutoConfig meta;
    private final int accent;

    private ConfigPanel panel;
    private CategorySidebar sidebar;
    private SearchBox searchBox;

    private ConfigPopup popup;

    private final Map<Field, Object> snapshot = new LinkedHashMap<>();

    // Preserved across resizes so the screen rebuilds in the same state
    private String selectedCategory = null;
    private String searchQuery = "";

    private final long openedAt = Util.getMillis();

    private int titleX = 20;
    private int contentRight = 0;
    private int footerY = 0;
    private Component headerTitle = Component.empty();
    private String headerDescription = "";
    private float resultsAnim = 0f;
    private float changesAnim = 0f;
    private int shownChanges = 0;

    static final int HEADER_HEIGHT = 46;
    static final int MARGIN = 12;
    private static final int DEFAULT_ACCENT = 0xFF4A9EFF;

    static final int BACKGROUND = 0x0E0E10;
    static final int SURFACE = 0x131316;
    static final int SURFACE_BORDER = 0x1F1F24;
    static final int ROW = 0x18181C;
    static final int ROW_HOVER = 0x1E1E23;
    static final int FIELD = 0x222228;
    static final int FIELD_HOVER = 0x2A2A31;
    static final int FIELD_BORDER = 0x2E2E35;
    static final int FIELD_BORDER_HOVER = 0x3C3C44;
    static final int INPUT = 0x0D0D10;
    static final int INPUT_HOVER = 0x111114;
    static final int INPUT_BORDER = 0x2A2A31;
    static final int TEXT = 0xECECEE;
    static final int TEXT_LABEL = 0xD2D2D6;
    static final int TEXT_SOFT = 0xA8A8AE;
    static final int TEXT_DIM = 0x8C8C93;
    static final int TEXT_MUTED = 0x6E6E75;
    static final int TEXT_FAINT = 0x4A4A50;
    static final int WARNING = 0xE89A52;
    static final int ERROR = 0xD65050;

    static final TextureIcon ICON_SEARCH = new TextureIcon("icon_search", 9, 9);
    static final TextureIcon ICON_RESET = new TextureIcon("icon_reload", 9, 9);
    static final TextureIcon ICON_RESTART = new TextureIcon("icon_restart", 9, 9);

    static final Icon ICON_CLEAR = new Icon(
            "#...#",
            ".#.#.",
            "..#..",
            ".#.#.",
            "#...#"
    );

    static final Icon ICON_CHEVRON_LEFT = new Icon(
            "..#",
            ".#.",
            "#..",
            ".#.",
            "..#"
    );

    static final Icon ICON_CHEVRON_RIGHT = ICON_CHEVRON_LEFT.mirrored();

    static final Icon ICON_CHEVRON_DOWN = new Icon(
            "#...#",
            ".#.#.",
            "..#.."
    );

    static final Icon ICON_CHEVRON_UP = ICON_CHEVRON_DOWN.flipped();

    static final Icon ICON_CHECK = new Icon(
            "....#",
            "...#.",
            "#.#..",
            ".#..."
    );

    static final Icon ICON_PLUS = new Icon(
            "..#..",
            "..#..",
            "#####",
            "..#..",
            "..#.."
    );

    // Seconds since the previous frame
    static float dt = 0f;
    private static long lastFrameNanos = 0L;

    private final Minecraft minecraft = Minecraft.getInstance();

    public KnightLibConfigScreen(Screen parent, Class<?> configClass) {
        super(Component.literal(resolveTitle(configClass)));
        this.parent = parent;
        this.configClass = configClass;
        this.meta = configClass.getAnnotation(AutoConfig.class);
        this.accent = meta != null ? meta.accentColor() : DEFAULT_ACCENT;
        takeSnapshot();
    }

    private void takeSnapshot() {
        for (Field field : configClass.getDeclaredFields()) {
            if (field.getAnnotation(ConfigEntry.class) == null) {
                continue;
            }

            field.setAccessible(true);
            try {
                // Lists are copied, the field's list could be edited in place by its mod
                final Object value = field.get(null);
                snapshot.put(field, value instanceof List<?> list ? new ArrayList<>(list) : value);
            }
            catch (Exception ignored) {
                ;;
            }

        }

    }

    private void restoreSnapshot() {
        for (Map.Entry<Field, Object> entry : snapshot.entrySet()) {
            try {
                ConfigManager.setPrimitive(entry.getKey(), entry.getValue());
            }
            catch (Exception ignored) {
                ;;
            }

        }

    }

    @Override
    protected void init() {
        resetFrameClock();
        this.popup = null;

        final int panelTop = HEADER_HEIGHT + 6;
        final int panelBottom = this.height - 40;

        final int contentLeft = MARGIN;
        this.contentRight = this.width - MARGIN;
        this.footerY = this.height - 30;

        final List<Field> entryFields = new ArrayList<>();
        final Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        for (Field field : configClass.getDeclaredFields()) {
            ConfigEntry entry = field.getAnnotation(ConfigEntry.class);
            if (entry == null) {
                continue;
            }

            field.setAccessible(true);
            entryFields.add(field);
            categoryCounts.merge(entry.category().trim(), 1, Integer::sum);
        }

        final boolean hasSidebar = categoryCounts.size() >= 2;
        final int sidebarWidth = hasSidebar ? CategorySidebar.preferredWidth(this.font, categoryCounts, entryFields.size(), this.width / 4) : 0;
        final int panelLeft = hasSidebar ? contentLeft + sidebarWidth + 8 : contentLeft;

        // The title lines up with the first column of text below it
        this.titleX = hasSidebar ? contentLeft + CategorySidebar.TEXT_INSET : panelLeft + ConfigPanel.PADDING + EntryRow.TEXT_INSET;
        this.panel = new ConfigPanel(panelLeft, panelTop, this.contentRight - panelLeft, Math.max(0, panelBottom - panelTop), accent);

        if (hasSidebar) {
            this.sidebar = new CategorySidebar(contentLeft, panelTop, sidebarWidth, Math.max(0, panelBottom - panelTop), accent, categoryCounts, entryFields.size(),
                    key -> {
                        selectedCategory = key;
                        panel.setCategory(key);
                    });
            this.sidebar.setSelectedKey(selectedCategory);
            this.addRenderableWidget(this.sidebar);
        }
        else {
            this.sidebar = null;
            this.selectedCategory = null;
        }

        this.addRenderableWidget(this.panel);

        final int searchWidth = Math.min(200, this.width / 3);
        final int searchX = this.contentRight - searchWidth;
        final int searchY = (HEADER_HEIGHT - 16) / 2 - 1;

        this.searchBox = new SearchBox(this.font, searchX, searchY, searchWidth, 16, accent);
        this.searchBox.setResponder(q -> {
            searchQuery = q;
            if (this.panel != null) {
                this.panel.setQuery(q);
            }
            if (this.sidebar != null) {
                this.sidebar.setDimmed(!q.isBlank());
            }

        });
        this.addRenderableWidget(this.searchBox);

        final int headerWidth = searchX - titleX - 12;
        final String description = meta != null ? meta.description() : "";
        this.headerTitle = ellipsize(this.font, resolveTitle(configClass), Style.EMPTY.withBold(true), headerWidth);
        this.headerDescription = description.isEmpty() ? "" : ellipsize(this.font, description, headerWidth);

        for (Field field : entryFields) {
            this.panel.addEntry(field, field.getAnnotation(ConfigEntry.class), snapshot.get(field));
        }

        this.panel.setCategory(selectedCategory);
        if (!searchQuery.isEmpty()) {
            this.searchBox.setValue(searchQuery);
        }

        final int buttonWidth = 90;
        final int buttonHeight = 18;
        final int gap = 6;
        final int totalWidth = buttonWidth * 3 + gap * 2;
        final int sx = (this.width - totalWidth) / 2;
        final int buttonY = this.footerY;

        addRenderableWidget(new FlatButton(sx, buttonY, buttonWidth, buttonHeight,
                CommonComponents.GUI_DONE,
                button -> {
                    panel.applyToFields(); ConfigManager.save(configClass); minecraft.setScreen(parent);
                },
                accent, true).withEntranceDelay(160));

        addRenderableWidget(new FlatButton(sx + buttonWidth + gap, buttonY, buttonWidth, buttonHeight,
                Component.translatable("knightlib.config.cancel"),
                button -> {
                    restoreSnapshot(); minecraft.setScreen(parent);
                },
                accent).withEntranceDelay(200));

        addRenderableWidget(new FlatButton(sx + (buttonWidth + gap) * 2, buttonY, buttonWidth, buttonHeight,
                Component.translatable("knightlib.config.reset_all"),
                button -> panel.resetAllToDefault(),
                accent).withEntranceDelay(240));
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        if (panel != null) {
            panel.applyToFields();
        }

        super.resize(minecraft, width, height);
    }

    @Override
    public void tick() {
        if (panel != null) {
            panel.tickPanel();
        }

        super.tick();
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (popup != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                popup.close();
            }
            else if (!popup.isClosing()) {
                popup.keyPressed(key, scancode, modifiers);
            }

            return true;
        }

        // Escape while typing in a row just stops typing, a second one leaves the screen
        if (key == GLFW.GLFW_KEY_ESCAPE && panel != null && panel.releaseEditing()) {
            return true;
        }

        if (key == GLFW.GLFW_KEY_F && hasControlDown() && searchBox != null) {
            this.setFocused(searchBox);
            searchBox.setFocused(true);
            return true;
        }

        return super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // A click outside the popup only closes it
        if (popup != null) {
            if (!popup.isClosing() && !popup.mouseClicked(mx, my, button)) {
                popup.close();
            }

            return true;
        }

        // Clicking anywhere outside the list closes the field being edited
        if (panel != null && !panel.isMouseOver(mx, my)) {
            panel.releaseEditing();
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (popup != null) {
            popup.mouseReleased(mx, my, button);
            return true;
        }

        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (popup != null) {
            popup.mouseDragged(mx, my, button, dx, dy);
            return true;
        }

        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (popup != null) {
            popup.mouseScrolled(mx, my, scrollY);
            return true;
        }

        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (popup != null) {
            if (!popup.isClosing()) {
                popup.charTyped(c, modifiers);
            }

            return true;
        }

        return super.charTyped(c, modifiers);
    }

    @Override
    public void onClose() {
        panel.applyToFields(); ConfigManager.save(configClass); minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        advanceFrame();

        graphics.fill(0, 0, width, height, opaque(BACKGROUND));

        renderHeader(graphics);

        // While a popup is open nothing underneath reacts to the cursor
        final boolean blocked = popup != null;
        super.render(graphics, blocked ? -10000 : mouseX, blocked ? -10000 : mouseY, partialTick);

        renderChanges(graphics);

        if (panel != null && !blocked) {
            panel.renderPendingTooltip(graphics, font, width, height);
        }

        if (popup != null) {
            popup.render(graphics, font, mouseX, mouseY);
            if (popup.isGone()) {
                popup = null;
            }

        }

    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ;;
    }

    private void renderHeader(GuiGraphics graphics) {
        final boolean hasDescription = !headerDescription.isEmpty();
        final int titleY = hasDescription ? 13 : (HEADER_HEIGHT - font.lineHeight) / 2;

        final long elapsed = Util.getMillis() - openedAt;
        drawRising(graphics, font, headerTitle, titleX, titleY, 0xFFFFFF, ease(elapsed / 380f));

        if (hasDescription) {
            drawRising(graphics, font, headerDescription, titleX, titleY + font.lineHeight + 3, TEXT_MUTED, ease((elapsed - 70) / 380f));
        }

        resultsAnim = approach(resultsAnim, searchQuery.isBlank() ? 0f : 1f, 14f);
        if (resultsAnim > 0.02f && panel != null && searchBox != null) {
            final int results = panel.visibleCount();
            final String text = I18n.get(results == 1 ? "knightlib.config.result" : "knightlib.config.results", results);
            drawRising(graphics, font, text, searchBox.getX() + searchBox.getWidth() - font.width(text), searchBox.getY() + searchBox.getHeight() + 4, TEXT_MUTED, resultsAnim);
        }

    }

    private void renderChanges(GuiGraphics graphics) {
        if (panel == null) {
            return;
        }

        final int modified = panel.countModified();
        if (modified > 0) {
            shownChanges = modified;
        }

        changesAnim = approach(changesAnim, modified > 0 ? 1f : 0f, 12f);
        if (changesAnim < 0.02f) {
            return;
        }

        final String text = I18n.get(shownChanges == 1 ? "knightlib.config.change" : "knightlib.config.changes", shownChanges);

        graphics.pose().pushPose();
        graphics.pose().translate((1f - changesAnim) * 6f, 0f, 0f);
        graphics.drawString(font, text, contentRight - font.width(text) + 1, footerY + 5, withAlpha(TEXT_MUTED, (int) (0xFF * changesAnim)), false);
        graphics.pose().popPose();
    }

    static String resolveTitle(Class<?> clazz) {
        AutoConfig autoConfig = clazz.getAnnotation(AutoConfig.class);
        if (autoConfig != null && !autoConfig.title().isEmpty()) {
            return autoConfig.title();
        }
        if (autoConfig != null) {
            return prettify(autoConfig.file());
        }

        return clazz.getSimpleName();
    }

    static String prettify(String name) {
        String[] parts = name.replace('_', ' ').replace('-', ' ').split("\\s+");
        StringBuilder stringBuilder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (!stringBuilder.isEmpty()) {
                stringBuilder.append(' ');
            }

            stringBuilder.append(Character.toUpperCase(part.charAt(0)));

            if (part.length() > 1) {
                stringBuilder.append(part.substring(1).toLowerCase());
            }

        }

        return stringBuilder.toString();
    }

    static String componentLabel(String name) {
        return prettify(name.replaceAll("([a-z0-9])([A-Z])", "$1_$2"));
    }

    static String ellipsize(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    static Component ellipsize(Font font, String text, Style style, int maxWidth) {
        final Component full = Component.literal(text).withStyle(style);
        if (font.width(full) <= maxWidth) {
            return full;
        }

        String cut = text;
        while (!cut.isEmpty() && font.width(Component.literal(cut + "...").withStyle(style)) > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }

        return Component.literal(cut.stripTrailing() + "...").withStyle(style);
    }

    static void advanceFrame() {
        final long now = Util.getNanos();
        dt = lastFrameNanos == 0L ? 0f : Mth.clamp((now - lastFrameNanos) / 1.0E9f, 0f, 0.1f);
        lastFrameNanos = now;
    }

    static void resetFrameClock() {
        lastFrameNanos = 0L;
    }

    static float approach(float current, float target, float speed) {
        final float next = current + (target - current) * (1f - (float) Math.exp(-speed * dt));
        return Math.abs(target - next) < 0.001f ? target : next;
    }

    static float ease(float progress) {
        return KnightLibEasings.EASE_OUT_QUART.apply(Mth.clamp(progress, 0f, 1f));
    }

    static float smooth(float progress) {
        return KnightLibEasings.SMOOTHSTEP.apply(Mth.clamp(progress, 0f, 1f));
    }

    static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    static int onAccent(int accent) {
        final int red = (accent >> 16) & 0xFF;
        final int green = (accent >> 8) & 0xFF;
        final int blue = accent & 0xFF;
        return (red * 299 + green * 587 + blue * 114) / 1000 > 150 ? 0x141417 : 0xFFFFFF;
    }

    static String displayLabel(Object value, boolean registryId) {
        if (value instanceof Enum<?> constant) {
            return prettify(constant.name());
        }

        if (value instanceof Double || value instanceof Float) {
            final double number = ((Number) value).doubleValue();
            return number == Math.floor(number) && !Double.isInfinite(number) ? String.valueOf((long) number) : String.valueOf(value);
        }

        final String text = String.valueOf(value);
        if (registryId && text.startsWith("#minecraft:")) {
            return "#" + text.substring("#minecraft:".length());
        }

        return registryId && text.startsWith("minecraft:") ? text.substring("minecraft:".length()) : text;
    }

    static void showPopup(ConfigPopup popup) {
        if (Minecraft.getInstance().screen instanceof KnightLibConfigScreen screen) {
            screen.popup = popup;
            popup.open(screen.width, screen.height);
        }

    }

    static boolean isPopupOpenFor(Object owner) {
        return Minecraft.getInstance().screen instanceof KnightLibConfigScreen screen && screen.popup != null && !screen.popup.isClosing() && screen.popup.owner() == owner;
    }

    static int withAlpha(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    static int mixRgb(int from, int to, float t) {
        final int red = (int) (((from >> 16) & 0xFF) + ((((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t));
        final int green = (int) (((from >> 8) & 0xFF) + ((((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t));
        final int blue = (int) ((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * t));
        return (red << 16) | (green << 8) | blue;
    }

    static void rounded(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color, int radius) {
        if (radius <= 0 || x1 - x0 <= radius * 2 || y1 - y0 <= radius * 2) {
            graphics.fill(x0, y0, x1, y1, color);
            return;
        }

        graphics.fill(x0 + radius, y0, x1 - radius, y1, color);
        for (int i = 0; i < radius; i++) {
            final int inset = radius - i;
            graphics.fill(x0 + i, y0 + inset, x0 + i + 1, y1 - inset, color);
            graphics.fill(x1 - i - 1, y0 + inset, x1 - i, y1 - inset, color);
        }

    }

    static void outline(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0 + 1, y0, x1 - 1, y0 + 1, color);
        graphics.fill(x0 + 1, y1 - 1, x1 - 1, y1, color);
        graphics.fill(x0, y0 + 1, x0 + 1, y1 - 1, color);
        graphics.fill(x1 - 1, y0 + 1, x1, y1 - 1, color);
    }

    static void frame(GuiGraphics graphics, int x0, int y0, int x1, int y1, int fill, int border, int radius) {
        if (radius < 2) {
            graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill);
            outline(graphics, x0, y0, x1, y1, border);
            return;
        }

        rounded(graphics, x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill, 1);

        graphics.fill(x0 + 2, y0, x1 - 2, y0 + 1, border);
        graphics.fill(x0 + 2, y1 - 1, x1 - 2, y1, border);
        graphics.fill(x0, y0 + 2, x0 + 1, y1 - 2, border);
        graphics.fill(x1 - 1, y0 + 2, x1, y1 - 2, border);

        graphics.fill(x0 + 1, y0 + 1, x0 + 2, y0 + 2, border);
        graphics.fill(x1 - 2, y0 + 1, x1 - 1, y0 + 2, border);
        graphics.fill(x0 + 1, y1 - 2, x0 + 2, y1 - 1, border);
        graphics.fill(x1 - 2, y1 - 2, x1 - 1, y1 - 1, border);
    }

    static void enableScissor(GuiGraphics graphics, int x0, int y0, int x1, int y1) {
        final Matrix4f pose = graphics.pose().last().pose();
        final int dx = Math.round(pose.m30());
        final int dy = Math.round(pose.m31());
        graphics.enableScissor(x0 + dx, y0 + dy, x1 + dx, y1 + dy);
    }

    static void beginFade(GuiGraphics graphics, float alpha) {
        if (alpha < 0.999f) {
            graphics.setColor(1f, 1f, 1f, Mth.clamp(alpha, 0f, 1f));
        }

    }

    static void endFade(GuiGraphics graphics, float alpha) {
        if (alpha < 0.999f) {
            graphics.setColor(1f, 1f, 1f, 1f);
        }

    }

    static void drawRising(GuiGraphics graphics, Font font, String text, int x, int y, int color, float progress) {
        drawRising(graphics, font, Component.literal(text), x, y, color, progress);
    }

    static void drawRising(GuiGraphics graphics, Font font, Component text, int x, int y, int color, float progress) {
        final int alpha = (int) (0xFF * progress);
        if (alpha < 0x08) {
            return;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0f, (1f - progress) * 4f, 0f);
        graphics.drawString(font, text, x, y, withAlpha(color, alpha), false);
        graphics.pose().popPose();
    }

    record TooltipLine(
            Component text,
            TextureIcon icon,
            int iconColor
    ) {

        static TooltipLine of(Component text) {
            return new TooltipLine(text, null, 0);
        }

    }

    static void drawTooltip(GuiGraphics graphics, Font font, List<TooltipLine> lines, int mouseX, int mouseY, float fade, int screenWidth, int screenHeight) {
        final int alpha = (int) (0xFF * fade);
        if (alpha < 0x08) {
            return;
        }

        // A line with an icon wraps past it, so its text lines up whatever row it continues on
        record Row(FormattedCharSequence text, int indent, TextureIcon icon, int iconColor) {}

        final int maxTextWidth = 200;
        final List<Row> rows = new ArrayList<>();
        int textWidth = 0;
        for (TooltipLine line : lines) {
            final int indent = line.icon() == null ? 0 : line.icon().width + 3;
            final List<FormattedCharSequence> split = font.split(line.text(), maxTextWidth - indent);
            for (int i = 0; i < split.size(); i++) {
                rows.add(new Row(split.get(i), indent, i == 0 ? line.icon() : null, line.iconColor()));
                textWidth = Math.max(textWidth, indent + font.width(split.get(i)));
            }

        }

        final int paddingX = 6;
        final int paddingY = 5;
        final int boxWidth = textWidth + paddingX * 2;
        final int boxHeight = rows.size() * (font.lineHeight + 1) - 1 + paddingY * 2;

        int x = mouseX + 10;
        int y = mouseY - boxHeight - 4;
        if (x + boxWidth > screenWidth - 4) {
            x = screenWidth - 4 - boxWidth;
        }
        if (y < 4) {
            y = mouseY + 12;
        }
        if (y + boxHeight > screenHeight - 4) {
            y = screenHeight - 4 - boxHeight;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0f, (1f - fade) * 3f, 400f);

        frame(graphics, x, y, x + boxWidth, y + boxHeight, withAlpha(0x17171A, (int) (0xF4 * fade)), withAlpha(0x2C2C32, alpha), 1);

        int lineY = y + paddingY;
        for (Row row : rows) {
            // One pixel up, so the icon is centered on the capitals like the badge next to the entry name
            if (row.icon() != null) {
                row.icon().draw(graphics, x + paddingX, lineY - 1, withAlpha(row.iconColor(), alpha));
            }

            graphics.drawString(font, row.text(), x + paddingX + row.indent(), lineY, withAlpha(0xDDDDE0, alpha), false);
            lineY += font.lineHeight + 1;
        }

        graphics.pose().popPose();
    }

    static void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
    }

    static final class Icon {

        final int width;
        final int height;
        private final String[] rows;

        Icon(String... rows) {
            this.rows = rows;
            this.height = rows.length;
            this.width = rows[0].length();
        }

        Icon mirrored() {
            final String[] flipped = new String[rows.length];
            for (int i = 0; i < rows.length; i++) {
                flipped[i] = new StringBuilder(rows[i]).reverse().toString();
            }

            return new Icon(flipped);
        }

        Icon flipped() {
            final String[] flipped = new String[rows.length];
            for (int i = 0; i < rows.length; i++) {
                flipped[i] = rows[rows.length - 1 - i];
            }

            return new Icon(flipped);
        }

        void draw(GuiGraphics graphics, int x, int y, int color) {
            for (int row = 0; row < rows.length; row++) {
                final String line = rows[row];
                int start = -1;
                for (int col = 0; col <= line.length(); col++) {
                    final boolean on = col < line.length() && line.charAt(col) == '#';
                    if (on && start < 0) {
                        start = col;
                    }
                    else if (!on && start >= 0) {
                        graphics.fill(x + start, y + row, x + col, y + row + 1, color);
                        start = -1;
                    }

                }

            }

        }

    }

    static final class TextureIcon {

        final int width;
        final int height;
        private final ResourceLocation texture;

        TextureIcon(String name, int width, int height) {
            this.texture = KnightLib.of("textures/gui/" + name + ".png");
            this.width = width;
            this.height = height;
        }

        void draw(GuiGraphics graphics, int x, int y, int color) {
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.enableBlend();

            final Matrix4f pose = graphics.pose().last().pose();
            final BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            buffer.addVertex(pose, x, y, 0f).setUv(0f, 0f).setColor(color);
            buffer.addVertex(pose, x, y + height, 0f).setUv(0f, 1f).setColor(color);
            buffer.addVertex(pose, x + width, y + height, 0f).setUv(1f, 1f).setColor(color);
            buffer.addVertex(pose, x + width, y, 0f).setUv(1f, 0f).setColor(color);
            BufferUploader.drawWithShader(buffer.buildOrThrow());

            RenderSystem.disableBlend();
        }

    }

    static final class Spring {

        float value;
        float velocity;
        private boolean initialized = false;

        void snap(float target) {
            value = target;
            velocity = 0f;
            initialized = true;
        }

        float update(float target, float stiffness, float dampingRatio) {
            if (!initialized) {
                snap(target);
                return value;
            }

            final float damping = 2f * (float) Math.sqrt(stiffness) * dampingRatio;
            float remaining = dt;
            while (remaining > 0f) {
                final float step = Math.min(remaining, 1f / 240f);
                velocity += ((target - value) * stiffness - velocity * damping) * step;
                value += velocity * step;
                remaining -= step;
            }

            if (Math.abs(target - value) < 0.001f && Math.abs(velocity) < 0.01f) {
                value = target;
                velocity = 0f;
            }

            return value;
        }

    }

    static class CategorySidebar extends AbstractWidget {

        private record Item(
                String key,
                String label,
                int count)
        {
            ;;
        }

        private final List<Item> items = new ArrayList<>();
        private final float[] hoverAnims;
        private final float[] selectAnims;
        private final int accent;
        private final Consumer<String> onSelect;
        private final long revealStart = Util.getMillis();

        private final Spring highlightTop = new Spring();
        private final Spring highlightBottom = new Spring();

        private int selected = 0;
        private boolean dimmed = false;
        private float dimAnim = 0f;
        private double scroll = 0;

        static final int ITEM_HEIGHT = 19;
        static final int ITEM_GAP = 1;
        private static final int INSET = 3;
        static final int TEXT_INSET = INSET + 6;

        CategorySidebar(int x, int y, int width, int height, int accent, Map<String, Integer> categoryCounts, int total, Consumer<String> onSelect) {
            super(x, y, width, height, Component.empty());
            this.accent = accent;
            this.onSelect = onSelect;

            items.add(new Item(null, I18n.get("knightlib.config.category.all"), total));
            for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                items.add(new Item(entry.getKey(), labelFor(entry.getKey()), entry.getValue()));
            }

            this.hoverAnims = new float[items.size()];
            this.selectAnims = new float[items.size()];
        }

        static String labelFor(String key) {
            return key.isEmpty() ? I18n.get("knightlib.config.category.general") : prettify(key);
        }

        static int preferredWidth(Font font, Map<String, Integer> categoryCounts, int total, int limit) {
            int widest = font.width(I18n.get("knightlib.config.category.all")) + font.width(String.valueOf(total));
            for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                widest = Math.max(widest, font.width(labelFor(entry.getKey())) + font.width(String.valueOf(entry.getValue())));
            }

            return Mth.clamp(widest + TEXT_INSET * 2 + 12, 80, Math.max(80, limit));
        }

        void setSelectedKey(String key) {
            selected = 0;
            for (int i = 0; i < items.size(); i++) {
                if (Objects.equals(items.get(i).key, key)) {
                    selected = i;
                    break;
                }

            }

            selectAnims[selected] = 1f;
        }

        void setDimmed(boolean dimmed) {
            this.dimmed = dimmed;
        }

        private int contentHeight() {
            return INSET * 2 + items.size() * (ITEM_HEIGHT + ITEM_GAP) - ITEM_GAP;
        }

        private boolean scrollable() {
            return contentHeight() > height;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            if (!visible) {
                return;
            }

            final Font font = Minecraft.getInstance().font;
            final int x = getX();
            final int y = getY();
            final int right = x + width;
            final int bottom = y + height;
            final long now = Util.getMillis();

            dimAnim = approach(dimAnim, dimmed ? 1f : 0f, 10f);
            final float dim = 1f - 0.6f * dimAnim;

            frame(graphics, x, y, right, bottom, opaque(SURFACE), opaque(SURFACE_BORDER), 2);
            graphics.enableScissor(x + 1, y + 1, right - 1, bottom - 1);

            final int itemLeft = x + INSET;
            final int itemRight = right - INSET;

            final float targetTop = selected * (ITEM_HEIGHT + ITEM_GAP);
            final boolean goingDown = targetTop > highlightTop.value + 0.01f;
            highlightTop.update(targetTop, goingDown ? 150f : 420f, goingDown ? 1f : 0.8f);
            highlightBottom.update(targetTop + ITEM_HEIGHT, goingDown ? 420f : 150f, goingDown ? 0.8f : 1f);

            final int highlightAlpha = (int) (0xFF * ease((now - revealStart) / 320f) * dim);
            if (highlightAlpha > 4) {
                final float topY = y + INSET + highlightTop.value - (float) scroll;
                final int top = Mth.floor(topY);
                final int size = Math.max(2, Math.round(highlightBottom.value - highlightTop.value));

                graphics.pose().pushPose();
                graphics.pose().translate(0f, topY - top, 0f);
                rounded(graphics, itemLeft, top, itemRight, top + size, withAlpha(mixRgb(SURFACE, accent, 0.2f), highlightAlpha), 1);
                graphics.pose().popPose();
            }

            int itemY = y + INSET - (int) scroll;
            for (int i = 0; i < items.size(); i++) {
                final Item item = items.get(i);

                final float entrance = ease((now - revealStart - 40L - i * 30L) / 320f);
                final boolean isSelected = i == selected;
                final boolean hovered = !dimmed && mx >= x && mx < right && my >= Math.max(itemY, y) && my < Math.min(itemY + ITEM_HEIGHT, bottom);

                hoverAnims[i] = approach(hoverAnims[i], hovered && !isSelected ? 1f : 0f, hovered ? 18f : 8f);
                selectAnims[i] = approach(selectAnims[i], isSelected ? 1f : 0f, 12f);
                final float hover = smooth(hoverAnims[i]);
                final float select = selectAnims[i];

                if (hover > 0.01f) {
                    rounded(graphics, itemLeft, itemY, itemRight, itemY + ITEM_HEIGHT, withAlpha(0xFFFFFF, (int) (0x09 * hover * entrance * dim)), 1);
                }

                final int textAlpha = (int) (0xFF * entrance * dim);
                if (textAlpha >= 0x08) {
                    // Offset by hand instead of translating, the scrolling label sets its own scissor
                    final int rise = Math.round((1f - entrance) * 4f);
                    final int textY = itemY + rise + (ITEM_HEIGHT - font.lineHeight) / 2 + 1;
                    final int labelColor = mixRgb(mixRgb(TEXT_DIM, TEXT_SOFT, hover), TEXT, select);

                    final String count = String.valueOf(item.count);
                    final int countX = right - TEXT_INSET - font.width(count) + 1;

                    // Long labels scroll back and forth
                    final int labelX = x + TEXT_INSET;
                    final int labelMaxX = countX - 4;
                    if (font.width(item.label) <= labelMaxX - labelX) {
                        graphics.drawString(font, item.label, labelX, textY, withAlpha(labelColor, textAlpha), false);
                    }
                    else {
                        renderScrollingString(graphics, font, Component.literal(item.label), labelX, itemY + rise, labelMaxX, itemY + rise + ITEM_HEIGHT, withAlpha(labelColor, textAlpha));
                    }

                    graphics.drawString(font, count, countX, textY, withAlpha(mixRgb(TEXT_FAINT, TEXT_SOFT, select), textAlpha), false);
                }

                itemY += ITEM_HEIGHT + ITEM_GAP;
            }

            graphics.disableScissor();
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!visible || dimmed || button != 0 || mx < getX() || mx >= getX() + width || my < getY() || my >= getY() + height) {
                return false;
            }

            int itemY = getY() + INSET - (int) scroll;
            for (int i = 0; i < items.size(); i++) {
                if (my >= itemY && my < itemY + ITEM_HEIGHT) {
                    if (i != selected) {
                        selected = i;
                        playDownSound(Minecraft.getInstance().getSoundManager());
                        onSelect.accept(items.get(i).key);
                    }

                    return true;
                }

                itemY += ITEM_HEIGHT + ITEM_GAP;
            }

            return true;
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
            if (!visible || !scrollable() || mx < getX() || mx >= getX() + width || my < getY() || my >= getY() + height) {
                return false;
            }

            scroll = Mth.clamp(scroll - scrollY * 16.0, 0, Math.max(0, contentHeight() - height));

            return true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            ;;
        }

    }

    static class ConfigPanel extends AbstractWidget {

        private final List<EntryRow> allRows = new ArrayList<>();
        private final List<EntryRow> visibleRows = new ArrayList<>();

        private final Map<String, Float> headerY = new HashMap<>();
        private final Map<String, Long> headerAppear = new HashMap<>();

        private double scroll = 0;
        private double targetScroll = 0;
        private boolean draggingScrollbar = false;
        private int dragOffset = 0;
        private EntryRow focused = null;
        final int accent;

        private String query = "";
        private String category = null;
        private boolean firstReveal = true;
        private long emptySince = Util.getMillis();

        private List<TooltipLine> pendingTooltip = null;
        private EntryRow pendingTooltipRow = null;
        private EntryRow tooltipRow = null;
        private long tooltipSince = 0;
        private int tooltipX;
        private int tooltipY;

        private float scrollbarAnim = 0f;

        private static final int ROW_GAP = 2;
        static final int PADDING = 5;
        private static final int SCROLLBAR_ZONE = 10;
        private static final int CATEGORY_HEIGHT = 20;

        ConfigPanel(int x, int y, int width, int height, int accent) {
            super(x, y, width, height, Component.empty());
            this.accent = accent;
        }

        void tickPanel() {
            ;;
        }

        void addEntry(Field field, ConfigEntry configEntry, Object snapshotValue) {
            allRows.add(new EntryRow(field, configEntry, snapshotValue, accent));
            visibleRows.add(allRows.get(allRows.size() - 1));

            recalcLayout();
        }

        void setQuery(String query) {
            this.query = query == null ? "" : query;
            applyFilter(false);
        }

        void setCategory(String category) {
            this.category = category;
            applyFilter(true);
        }

        private void applyFilter(boolean cascade) {
            final Set<EntryRow> before = Collections.newSetFromMap(new IdentityHashMap<>());
            before.addAll(visibleRows);

            visibleRows.clear();

            final boolean searching = !query.isBlank();
            final String content = query.toLowerCase().replace('_', ' ');

            for (EntryRow entryRow : allRows) {
                if (searching) {
                    if (entryRow.matchesSearch(content)) {
                        visibleRows.add(entryRow);
                    }

                }
                else if (category == null || entryRow.categoryKey().equals(category)) {
                    visibleRows.add(entryRow);
                }

            }

            recalcLayout();

            final long now = Util.getMillis();
            if (cascade) {
                final long start = now + (firstReveal ? 90L : 0L);
                firstReveal = false;

                headerY.clear();
                headerAppear.clear();
                for (EntryRow row : allRows) {
                    row.layoutY = Float.NaN;
                }

                for (int i = 0; i < visibleRows.size(); i++) {
                    visibleRows.get(i).appearAt = start + Math.min(i, 14) * 26L;
                }

                emptySince = start;
                targetScroll = 0;
                scroll = 0;
                return;
            }

            final Set<EntryRow> after = Collections.newSetFromMap(new IdentityHashMap<>());
            after.addAll(visibleRows);

            for (EntryRow row : allRows) {
                if (!after.contains(row)) {
                    row.layoutY = Float.NaN;
                }

            }

            int fresh = 0;
            for (EntryRow row : visibleRows) {
                if (!before.contains(row)) {
                    row.layoutY = Float.NaN;
                    row.appearAt = now + Math.min(fresh++, 10) * 18L;
                }

            }

            if (visibleRows.isEmpty() && !before.isEmpty()) {
                emptySince = now;
            }

            targetScroll = 0;
        }

        private boolean showHeaders() {
            if (!query.isBlank() || category == null) {
                Set<String> keys = new HashSet<>();
                for (EntryRow entryRow : visibleRows) {
                    keys.add(entryRow.categoryKey());
                }

                return keys.size() > 1;
            }

            return false;
        }

        void applyToFields() {
            for (EntryRow entryRow : allRows) {
                entryRow.writeToField();
            }

        }

        void resetAllToDefault() {
            final long now = Util.getMillis();

            // Visible rows flash one after another, top to bottom
            int changed = 0;
            for (EntryRow entryRow : visibleRows) {
                if (entryRow.resetToDefault()) {
                    entryRow.flashAt = now + Math.min(changed++, 16) * 22L;
                }

            }

            final Set<EntryRow> shown = Collections.newSetFromMap(new IdentityHashMap<>());
            shown.addAll(visibleRows);
            for (EntryRow entryRow : allRows) {
                if (!shown.contains(entryRow)) {
                    entryRow.resetToDefault();
                }

            }

        }

        int visibleCount() {
            return visibleRows.size();
        }

        int countModified() {
            int count = 0;
            for (EntryRow entryRow : allRows) {
                if (entryRow.isModified()) {
                    count++;
                }

            }

            return count;
        }

        private int contentWidth() {
            return width - PADDING - SCROLLBAR_ZONE;
        }

        private int rowLeft() {
            return getX() + PADDING;
        }

        private int topPad() {
            return showHeaders() ? 2 : PADDING;
        }

        private void recalcLayout() {
            int width = contentWidth();
            for (EntryRow entryRow : visibleRows) {
                entryRow.recalcHeight(width);
            }

            clampScroll();
        }

        private int totalContentHeight() {
            final boolean headers = showHeaders();
            int height = topPad();
            String lastCategory = null;
            for (int i = 0; i < visibleRows.size(); i++) {
                if (i > 0) {
                    height += ROW_GAP;
                }

                String category = visibleRows.get(i).categoryKey();
                if (headers && !category.equals(lastCategory)) {
                    height += CATEGORY_HEIGHT;
                }

                lastCategory = category;
                height += visibleRows.get(i).fullHeight();
            }

            // Bottom margin
            return height + PADDING;
        }

        private boolean scrollable() {
            return totalContentHeight() > height;
        }

        private void clampScroll() {
            double max = Math.max(0, totalContentHeight() - height);
            targetScroll = Math.max(0, Math.min(max, targetScroll));
            scroll = Math.max(0, Math.min(max, scroll));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            if (!visible) {
                return;
            }

            pendingTooltip = null;
            pendingTooltipRow = null;

            for (EntryRow row : allRows) {
                row.tickExpand();
            }

            if (!draggingScrollbar && scroll != targetScroll) {
                final double remaining = targetScroll - scroll;
                scroll = Math.abs(remaining) < 0.3 ? targetScroll : scroll + remaining * (1.0 - Math.exp(-20.0 * dt));

                clampScroll();
            }

            final int x = getX();
            final int y = getY();
            final int right = x + width;
            final int bottom = y + height;
            frame(graphics, x, y, right, bottom, opaque(SURFACE), opaque(SURFACE_BORDER), 2);

            graphics.enableScissor(x + 1, y + 1, right - 1, bottom - 1);

            final long now = Util.getMillis();
            if (visibleRows.isEmpty()) {
                renderEmptyState(graphics, x, y, bottom, now);
            }

            final boolean headers = showHeaders();
            final boolean inPanel = isIn(mx, my);
            final int rowLeft = rowLeft();
            final int rowWidth = contentWidth();
            final Set<String> seenHeaders = new HashSet<>();

            float contentY = topPad();
            String lastCategory = null;

            int extra = 0;

            for (int i = 0; i < visibleRows.size(); i++) {
                final EntryRow row = visibleRows.get(i);
                if (i > 0) {
                    contentY += ROW_GAP;
                }

                final String category = row.categoryKey();
                if (headers && !category.equals(lastCategory)) {
                    if (!headerY.containsKey(category)) {
                        headerY.put(category, contentY);
                        headerAppear.put(category, Math.max(row.appearAt, now));
                    }

                    final float shown = approach(headerY.get(category), contentY, 18f);
                    headerY.put(category, shown);
                    seenHeaders.add(category);

                    final float headerTop = y + shown + extra - (float) scroll;
                    if (headerTop + CATEGORY_HEIGHT >= y && headerTop <= bottom) {
                        renderCategoryHeader(graphics, category, rowLeft, headerTop, ease((now - headerAppear.get(category)) / 300f));
                    }

                    contentY += CATEGORY_HEIGHT;
                }

                lastCategory = category;

                row.layoutY = Float.isNaN(row.layoutY) ? contentY : approach(row.layoutY, contentY, 18f);
                final float rowTop = y + row.layoutY + extra - (float) scroll;
                row.drawTop = Mth.floor(rowTop);

                final int rowH = row.fullHeight();
                if (rowTop + rowH >= y && rowTop <= bottom) {
                    final boolean hovered = inPanel && mx >= rowLeft && mx < rowLeft + rowWidth && my >= row.drawTop && my < row.drawTop + rowH;
                    final float appear = ease((now - row.appearAt) / 300f);
                    row.render(graphics, rowTop, rowLeft, rowWidth, mx, my, hovered, appear, now, partialTick);

                    if (hovered) {
                        captureTooltip(row, mx, my, rowLeft, rowWidth);
                    }

                }
                else {
                    row.hoverStart = 0;
                }

                extra += row.extraHeight();
                contentY += row.height;
            }

            headerY.keySet().retainAll(seenHeaders);
            headerAppear.keySet().retainAll(seenHeaders);

            graphics.disableScissor();

            // An opening text field near the bottom pulls the list up so it stays in view
            if (focused != null && focused.expansion() > 0f && visibleRows.contains(focused)) {
                final int overflow = focused.drawTop + focused.fullHeight() + PADDING - bottom;
                if (overflow > 0) {
                    targetScroll = Math.max(targetScroll, scroll + overflow);
                    clampScroll();
                }

            }

            if (pendingTooltipRow != tooltipRow) {
                tooltipRow = pendingTooltipRow;
                tooltipSince = Util.getMillis();
            }

            renderEdgeFades(graphics, x, y, right, bottom);

            renderScrollbar(graphics, mx, my);
        }

        private void renderEdgeFades(GuiGraphics graphics, int x, int y, int right, int bottom) {
            final double maxScroll = Math.max(0, totalContentHeight() - height);

            // Grows with the distance left to scroll, so it never pops in
            final float top = (float) Mth.clamp(scroll / 14.0, 0.0, 1.0);
            final float end = (float) Mth.clamp((maxScroll - scroll) / 14.0, 0.0, 1.0);

            if (top > 0.01f) {
                graphics.fillGradient(x + 2, y + 1, right - 2, y + 13, withAlpha(SURFACE, (int) (0xF0 * top)), withAlpha(SURFACE, 0));
            }

            if (end > 0.01f) {
                graphics.fillGradient(x + 2, bottom - 13, right - 2, bottom - 1, withAlpha(SURFACE, 0), withAlpha(SURFACE, (int) (0xF0 * end)));
            }

        }

        private void renderEmptyState(GuiGraphics graphics, int x, int y, int bottom, long now) {
            final Font font = Minecraft.getInstance().font;
            final String text = query.isBlank() ? I18n.get("knightlib.config.empty") : I18n.get("knightlib.config.empty_search", query);
            final int textY = y + (bottom - y - font.lineHeight) / 2;

            drawRising(graphics, font, text, x + (width - font.width(text)) / 2, textY, TEXT_MUTED, ease((now - emptySince) / 300f));
        }

        private void captureTooltip(EntryRow row, int mx, int my, int rowLeft, int rowWidth) {
            if (mx > rowLeft + rowWidth - row.widget.getWidth() - 30) {
                if (row.hoverStart == 0) {
                    row.hoverStart = Util.getMillis();
                }

                return;
            }

            if (row.hoverStart == 0) {
                row.hoverStart = Util.getMillis();
            }

            if (Util.getMillis() - row.hoverStart < 450) {
                return;
            }

            List<TooltipLine> lines = row.tooltipLines();
            if (!lines.isEmpty()) {
                pendingTooltip = lines;
                pendingTooltipRow = row;
                tooltipX = mx;
                tooltipY = my;
            }

        }

        void renderPendingTooltip(GuiGraphics graphics, Font font, int screenWidth, int screenHeight) {
            if (pendingTooltip == null || pendingTooltip.isEmpty()) {
                return;
            }

            drawTooltip(graphics, font, pendingTooltip, tooltipX, tooltipY, ease((Util.getMillis() - tooltipSince) / 160f), screenWidth, screenHeight);
        }

        private void renderCategoryHeader(GuiGraphics graphics, String category, int left, float top, float appear) {
            final Font font = Minecraft.getInstance().font;
            final String label = CategorySidebar.labelFor(category);
            final int alpha = (int) (0xFF * appear);
            if (alpha < 0x08) {
                return;
            }

            final int baseTop = Mth.floor(top);
            graphics.pose().pushPose();
            graphics.pose().translate(0f, (top - baseTop) + (1f - appear) * 3f, 0f);
            graphics.drawString(font, label, left + EntryRow.TEXT_INSET, baseTop + CATEGORY_HEIGHT - font.lineHeight - 4, withAlpha(TEXT_MUTED, alpha), false);
            graphics.pose().popPose();
        }

        private int trackTop() {
            return getY() + 4;
        }

        private int trackHeight() {
            return height - 8;
        }

        private int thumbHeight() {
            return Math.max(16, (int) (trackHeight() * ((double) height / totalContentHeight())));
        }

        private int thumbY() {
            final double maxScroll = Math.max(1, totalContentHeight() - height);
            return trackTop() + (int) ((trackHeight() - thumbHeight()) * (scroll / maxScroll));
        }

        private void renderScrollbar(GuiGraphics graphics, int mx, int my) {
            if (!scrollable()) {
                scrollbarAnim = 0f;
                return;
            }

            final boolean engaged = draggingScrollbar || Math.abs(targetScroll - scroll) > 0.5 || (isIn(mx, my) && mx >= getX() + width - SCROLLBAR_ZONE);
            scrollbarAnim = approach(scrollbarAnim, engaged ? 1f : 0f, engaged ? 16f : 3f);

            final int trackX = getX() + width - 7;
            if (scrollbarAnim > 0.01f) {
                rounded(graphics, trackX, trackTop(), trackX + 3, trackTop() + trackHeight(), withAlpha(0xFFFFFF, (int) (0x08 * scrollbarAnim)), 1);
            }

            final int thumbY = thumbY();
            rounded(graphics, trackX, thumbY, trackX + 3, thumbY + thumbHeight(), opaque(mixRgb(0x34343A, 0x7C7C84, scrollbarAnim)), 1);
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
            if (!visible || !isIn(mx, my) || !scrollable()) {
                return false;
            }

            draggingScrollbar = false;
            targetScroll -= scrollY * 28.0;

            clampScroll();

            return true;
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!visible || !active || !isIn(mx, my)) {
                return false;
            }

            if (scrollable() && mx >= getX() + width - SCROLLBAR_ZONE) {
                final int thumbY = thumbY();
                final int thumbH = thumbHeight();
                if (my >= thumbY && my <= thumbY + thumbH) {
                    draggingScrollbar = true;
                    dragOffset = (int) my - thumbY;
                }
                else {
                    final int span = Math.max(1, trackHeight() - thumbH);
                    final double t = Mth.clamp((my - trackTop() - thumbH / 2.0) / span, 0.0, 1.0);
                    targetScroll = t * Math.max(0, totalContentHeight() - height);

                    clampScroll();
                }

                return true;
            }

            for (EntryRow row : visibleRows) {
                if (my >= row.drawTop && my < row.drawTop + row.fullHeight() && mx >= rowLeft() && mx < rowLeft() + contentWidth()) {
                    if (row.clickedReset(mx, my, button)) {
                        clearFocused();
                        return true;
                    }

                    if (row.mouseClicked(mx, my, button)) {
                        setFocusedRow(row);
                        return true;
                    }

                    clearFocused();

                    return true;
                }

            }

            clearFocused();

            return false;
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            if (draggingScrollbar && scrollable()) {
                final int span = trackHeight() - thumbHeight();
                if (span <= 0) {
                    return true;
                }

                final double t = Mth.clamp((my - dragOffset - trackTop()) / span, 0.0, 1.0);

                targetScroll = t * Math.max(0, totalContentHeight() - height);
                scroll = targetScroll;

                clampScroll();

                return true;
            }

            return focused != null && focused.mouseDragged(mx, my, button, dx, dy);
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            if (draggingScrollbar) {
                draggingScrollbar = false;

                return true;
            }

            return focused != null && focused.mouseReleased(mx, my, button);
        }

        @Override
        public boolean keyPressed(int key, int s, int m) {
            // Enter closes an open text field, the slider handles its own Enter
            if (focused != null && focused.widget instanceof EditBox && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
                clearFocused();
                return true;
            }

            return (focused != null && focused.keyPressed(key, s, m)) || super.keyPressed(key, s, m);
        }

        boolean releaseEditing() {
            if (focused == null || !focused.isEditing()) {
                return false;
            }

            clearFocused();
            return true;
        }

        @Override
        public boolean charTyped(char c, int m) {
            return (focused != null && focused.charTyped(c, m)) || super.charTyped(c, m);
        }

        private boolean isIn(double mx, double my) {
            return mx >= getX() && mx <= getX() + width && my >= getY() && my <= getY() + height;
        }

        private void clearFocused() {
            if (focused != null) {
                focused.setFocused(false);
                focused = null;
            }

        }

        private void setFocusedRow(EntryRow entryRow) {
            if (focused == entryRow) {
                return;
            }

            if (focused != null) {
                focused.setFocused(false);
            }

            focused = entryRow;
            if (entryRow != null) {
                entryRow.setFocused(true);
            }

        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            ;;
        }

    }

    private static class EntryRow {

        static final int TEXT_INSET = 10;
        private static final int RESET_SIZE = 16;
        private static final int EXPAND_EXTRA = 22;

        final Field field;
        final ConfigEntry meta;
        final Component label;
        final String description;
        final AbstractWidget widget;
        final String searchIndex;
        final int accent;
        final Object snapshotValue;
        final Object defaultValue;

        float hoverAnim = 0f;
        private float modifiedAnim = 0f;
        private float resetAnim = 0f;
        private float resetHoverAnim = 0f;

        long hoverStart = 0;
        long appearAt = 0;
        long flashAt = 0;

        // Position inside the panel's content, eased towards its layout slot
        float layoutY = Float.NaN;
        int drawTop = 0;

        private int resetX;
        private int resetY;

        int height = 30;
        int descriptionLines = 0;
        int descriptionMaxWidth = 0;

        private List<TooltipLine> cachedTooltip = null;
        private List<FormattedCharSequence> descriptionCache = List.of();

        EntryRow(Field field, ConfigEntry meta, Object snapshotValue, int accent) {
            this.field = field;
            this.meta = meta;
            this.accent = accent;
            this.snapshotValue = snapshotValue;
            this.defaultValue = ConfigManager.getCodeDefault(field);
            this.label = Component.literal(prettify(field.getName()));
            this.description = meta.comment().trim();
            this.searchIndex = (field.getName() + " " + prettify(field.getName()) + " " + meta.category() + " " + description).toLowerCase().replace('_', ' ');
            this.widget = createWidget();
        }

        String categoryKey() {
            return meta.category().trim();
        }

        boolean matchesSearch(String query) {
            for (String chunk : query.split("\\s+")) {
                if (!chunk.isEmpty() && !searchIndex.contains(chunk)) {
                    return false;
                }

            }

            return true;
        }

        void setFocused(boolean focused) {
            widget.setFocused(focused);

            if (widget instanceof EditBox editBox) {
                editBox.setFocused(focused);
            }

        }

        boolean isEditing() {
            return (widget instanceof EditBox box && box.isFocused()) || (widget instanceof ConfigSlider slider && slider.editing);
        }

        void tickExpand() {
            if (widget instanceof StyledEditBox box) {
                box.updateExpand();
            }

        }

        float expansion() {
            return widget instanceof StyledEditBox box ? box.expandProgress() : 0f;
        }

        int extraHeight() {
            return Math.round(EXPAND_EXTRA * expansion());
        }

        int fullHeight() {
            return height + extraHeight();
        }

        private int restingWidgetWidth() {
            return widget instanceof StyledEditBox box ? box.baseWidth() : widget.getWidth();
        }

        void recalcHeight(int rowWidth) {
            final Minecraft minecraft = Minecraft.getInstance();
            final int lineHeight = minecraft.font.lineHeight;
            final int avail = rowWidth - restingWidgetWidth() - 54;

            descriptionMaxWidth = Math.min(avail, (int) (rowWidth * 0.5f));
            descriptionCache = (!description.isEmpty() && descriptionMaxWidth > 30) ? minecraft.font.split(Component.literal(description), descriptionMaxWidth) : List.of();
            descriptionLines = descriptionCache.size();

            final int contentHeight = lineHeight + (descriptionLines > 0 ? 2 + descriptionLines * lineHeight : 0);
            height = Math.max(Math.max(contentHeight + 14, widget.getHeight() + 12), 30);
        }

        private AbstractWidget createWidget() {
            final Minecraft minecraft = Minecraft.getInstance();
            final Class<?> type = field.getType();
            try {
                if (type == boolean.class) {
                    return new ToggleButton(0, 0, 30, 16, field.getBoolean(null), accent);
                }

                final boolean numeric = type == int.class || type == long.class || type == float.class || type == double.class;
                if (numeric && meta.slider() && Double.isFinite(meta.min()) && Double.isFinite(meta.max())) {
                    boolean isInt = type == int.class || type == long.class;
                    return new ConfigSlider(0, 0, ConfigSlider.preferredWidth(meta.min(), meta.max(), isInt), 18, meta.min(), meta.max(), ((Number) field.get(null)).doubleValue(), isInt, accent);
                }

                if (type.isRecord()) {
                    final RecordButton button = new RecordButton(150, type, meta, accent);
                    button.setFromField(field.get(null));
                    return button;
                }

                if (List.class.isAssignableFrom(type)) {
                    final Class<?> element = ConfigManager.listElementType(field);
                    final List<Object> choices = choicesFor(element, meta);
                    final SelectionButton button = new SelectionButton(150, true, element, choices, choices != null && pickedFromRegistry(meta), meta, accent);
                    button.setFromField(field.get(null));
                    return button;
                }

                if (type == String.class && (meta.options().length > 0 || !meta.registry().isEmpty())) {
                    final List<Object> choices = choicesFor(String.class, meta);
                    if (choices != null) {
                        final SelectionButton button = new SelectionButton(150, false, String.class, choices, pickedFromRegistry(meta), meta, accent);
                        button.setFromField(field.get(null));
                        return button;
                    }

                }

                if (type.isEnum()) {
                    final Enum<?>[] constants = (Enum<?>[]) type.getEnumConstants();
                    int widest = 0;
                    for (Enum<?> constant : constants) {
                        widest = Math.max(widest, minecraft.font.width(prettify(constant.name())));
                    }

                    // Cycling through more than a handful of values gets tedious, those get a dropdown
                    if (constants.length > 6) {
                        final SelectionButton button = new SelectionButton(Mth.clamp(widest + 30, 90, 160), false, type, new ArrayList<>(Arrays.asList(constants)), false, meta, accent);
                        button.setFromField(field.get(null));
                        return button;
                    }

                    return new EnumCycleButton(0, 0, Mth.clamp(widest + 32, 80, 160), 18, constants, (Enum<?>) field.get(null), accent);
                }

                final String value = String.valueOf(field.get(null));
                final boolean text = type == String.class;
                final StyledEditBox box = new StyledEditBox(minecraft.font, 0, 0, text ? (value.length() > 20 ? 200 : 150) : 80, 18, Component.empty(), accent, text);

                box.setMaxLength(512);
                box.setValue(value);
                box.setValidator(validatorFor(type));

                // An empty text field otherwise looks like a hole in the row
                if (text) {
                    box.setHint(Component.translatable("knightlib.config.empty_value").withStyle(style -> style.withColor(TEXT_FAINT)));
                }

                return box;
            }
            catch (Exception exception) {
                final EditBox box = new StyledEditBox(minecraft.font, 0, 0, 120, 18, Component.empty(), accent, false);
                box.setValue("");
                return box;
            }

        }

        private static boolean pickedFromRegistry(ConfigEntry meta) {
            return meta.options().length == 0 && !meta.registry().isEmpty();
        }

        private static List<Object> choicesFor(Class<?> element, ConfigEntry meta) {
            if (meta.options().length > 0) {
                final List<Object> choices = new ArrayList<>();
                for (String option : meta.options()) {
                    final Object value = ConfigManager.coerceElement(option, element, meta);
                    if (value != null && !choices.contains(value)) {
                        choices.add(value);
                    }

                }

                return choices;
            }

            if (element.isEnum()) {
                return new ArrayList<>(Arrays.asList(element.getEnumConstants()));
            }

            if (element == String.class && !meta.registry().isEmpty()) {
                return registryIds(meta.registry(), meta.tags());
            }

            return null;
        }

        private static List<Object> registryIds(String registryName, boolean withTags) {
            final ResourceLocation key = ResourceLocation.tryParse(registryName);
            if (key == null) {
                return null;
            }

            Registry<?> registry = BuiltInRegistries.REGISTRY.get(key);
            final ClientLevel level = Minecraft.getInstance().level;
            if (registry == null && level != null) {
                registry = level.registryAccess().registry(ResourceKey.createRegistryKey(key)).orElse(null);
            }

            if (registry == null) {
                return null;
            }

            final Comparator<ResourceLocation> vanillaFirst = Comparator.comparing((ResourceLocation location) -> !location.getNamespace().equals("minecraft")).thenComparing(ResourceLocation::getNamespace).thenComparing(ResourceLocation::getPath);

            final List<Object> ids = new ArrayList<>();
            if (withTags && level != null) {
                registry.getTagNames().map(TagKey::location).sorted(vanillaFirst).forEach(location -> ids.add("#" + location));
            }

            final List<ResourceLocation> keys = new ArrayList<>(registry.keySet());
            keys.sort(vanillaFirst);
            for (ResourceLocation location : keys) {
                ids.add(location.toString());
            }

            return ids;
        }

        private static Predicate<String> validatorFor(Class<?> type) {
            if (type == int.class) {
                return raw -> raw.isBlank() || parseIntSafe(raw, Integer.MIN_VALUE) != Integer.MIN_VALUE || raw.trim().equals(String.valueOf(Integer.MIN_VALUE));
            }
            if (type == long.class) {
                return raw -> raw.isBlank() || parseLongSafe(raw, Long.MIN_VALUE) != Long.MIN_VALUE || raw.trim().equals(String.valueOf(Long.MIN_VALUE));
            }
            if (type == float.class || type == double.class) {
                return raw -> {
                    if (raw.isBlank()) {
                        return true;
                    }
                    try {
                        Double.parseDouble(raw.trim());
                        return true;
                    }
                    catch (NumberFormatException exception) {
                        return false;
                    }

                };

            }

            return raw -> true;
        }

        private Object currentValue() {
            final Class<?> type = field.getType();
            if (widget instanceof ToggleButton toggleButton) {
                return toggleButton.isToggled();
            }
            if (widget instanceof ConfigSlider configSlider) {
                return configSlider.getValueRaw();
            }
            if (widget instanceof EnumCycleButton enumCycleButton) {
                return enumCycleButton.getCurrent();
            }
            if (widget instanceof SelectionButton selection) {
                return selection.fieldValue();
            }
            if (widget instanceof EditBox box) {
                final String raw = box.getValue().trim();
                if (type == String.class) {
                    return box.getValue();
                }
                if (raw.isEmpty()) {
                    return null;
                }
                try {
                    if (type == int.class) {
                        return Integer.decode(raw.startsWith("#") ? "0x" + raw.substring(1) : raw);
                    }
                    if (type == long.class) {
                        return Long.decode(raw.startsWith("#") ? "0x" + raw.substring(1) : raw);
                    }
                    if (type == float.class) {
                        return Float.parseFloat(raw);
                    }
                    if (type == double.class) {
                        return Double.parseDouble(raw);
                    }

                }
                catch (NumberFormatException exception) {
                    return null;
                }

            }

            return null;
        }

        private boolean differsFrom(Object reference) {
            if (reference == null) {
                return false;
            }

            final Object current = currentValue();
            if (current == null) {
                return false;
            }

            if (current instanceof Number number && reference instanceof Number referenceNumber) {
                return Math.abs(number.doubleValue() - referenceNumber.doubleValue()) > 1e-6;
            }

            return !current.equals(reference);
        }

        boolean isModified() {
            return differsFrom(snapshotValue);
        }

        private boolean canReset() {
            return defaultValue != null && (currentValue() == null || differsFrom(defaultValue));
        }

        List<TooltipLine> tooltipLines() {
            if (cachedTooltip != null) {
                return cachedTooltip;
            }

            final List<TooltipLine> lines = new ArrayList<>();
            final String note = meta.note().trim();
            if (!note.isEmpty()) {
                lines.add(TooltipLine.of(Component.literal(note)));
            }

            if (defaultValue != null) {
                lines.add(TooltipLine.of(Component.translatable("knightlib.config.tooltip.default", formatValue(defaultValue)).withStyle(style -> style.withColor(TEXT_DIM))));
            }

            if (Double.isFinite(meta.min()) && Double.isFinite(meta.max())) {
                final boolean isInt = field.getType() == int.class || field.getType() == long.class;
                final String min = isInt ? String.valueOf((long) meta.min()) : trimDouble(meta.min());
                final String max = isInt ? String.valueOf((long) meta.max()) : trimDouble(meta.max());
                lines.add(TooltipLine.of(Component.translatable("knightlib.config.tooltip.range", min, max).withStyle(style -> style.withColor(TEXT_DIM))));
            }

            if (meta.minSize() > 0 && List.class.isAssignableFrom(field.getType())) {
                lines.add(TooltipLine.of(Component.translatable("knightlib.config.tooltip.min_size", meta.minSize()).withStyle(style -> style.withColor(TEXT_DIM))));
            }

            if (meta.maxSize() > 0 && List.class.isAssignableFrom(field.getType())) {
                lines.add(TooltipLine.of(Component.translatable("knightlib.config.tooltip.max_size", meta.maxSize()).withStyle(style -> style.withColor(TEXT_DIM))));
            }

            if (widget instanceof ConfigSlider) {
                lines.add(TooltipLine.of(Component.translatable("knightlib.config.tooltip.slider_hint").withStyle(style -> style.withColor(TEXT_MUTED))));
            }

            if (meta.requiresRestart()) {
                lines.add(new TooltipLine(Component.translatable("knightlib.config.tooltip.requires_restart").withStyle(style -> style.withColor(WARNING)), ICON_RESTART, WARNING));
            }

            cachedTooltip = lines;

            return lines;
        }

        private static String formatValue(Object value) {
            if (value instanceof List<?> list) {
                if (list.isEmpty()) {
                    return I18n.get("knightlib.config.empty_value");
                }

                final List<String> parts = new ArrayList<>(list.size());
                for (Object element : list) {
                    parts.add(formatValue(element));
                }

                return String.join(", ", parts);
            }
            if (value instanceof String text && text.isEmpty()) {
                return I18n.get("knightlib.config.empty_value");
            }
            if (value instanceof Double d) {
                return trimDouble(d);
            }
            if (value instanceof Float f) {
                return trimDouble(f);
            }
            if (value instanceof Enum<?> e) {
                return prettify(e.name());
            }
            if (value instanceof Record record) {
                final RecordComponent[] components = record.getClass().getRecordComponents();
                final List<Object> values = ConfigManager.recordValues(record);
                final List<String> parts = new ArrayList<>(components.length);
                for (int i = 0; i < components.length; i++) {
                    parts.add(componentLabel(components[i].getName()) + " " + formatValue(values.get(i)));
                }

                return String.join(", ", parts);
            }

            return String.valueOf(value);
        }

        private static String trimDouble(double value) {
            if (value == Math.floor(value) && !Double.isInfinite(value)) {
                return String.valueOf((long) value);
            }

            return String.valueOf(value);
        }

        void writeToField() {
            try {
                final Class<?> type = field.getType();
                if (type == boolean.class && widget instanceof ToggleButton toggleButton) {
                    field.setBoolean(null, toggleButton.isToggled()); return;
                }
                if (widget instanceof ConfigSlider configSlider) {
                    double valueRaw = configSlider.getValueRaw();
                    if (type == int.class) {
                        field.setInt(null, (int) valueRaw);
                    }
                    else if (type == long.class) {
                        field.setLong(null, (long) valueRaw);
                    }
                    else if (type == float.class) {
                        field.setFloat(null, (float) valueRaw);
                    }
                    else {
                        field.setDouble(null, valueRaw);
                    }

                    return;
                }
                if (widget instanceof EnumCycleButton enumCycleButton) {
                    field.set(null, enumCycleButton.getCurrent()); return;
                }
                if (widget instanceof SelectionButton selection) {
                    final Object value = selection.fieldValue();
                    if (value != null) {
                        field.set(null, value);
                    }

                    return;
                }
                if (widget instanceof EditBox box) {
                    String raw = box.getValue().trim();
                    if (type == String.class) {
                        field.set(null, box.getValue());
                        return;
                    }

                    if (raw.isEmpty()) {
                        return;
                    }
                    if (type == int.class) {
                        field.setInt(null, parseIntSafe(raw, field.getInt(null)));
                    }
                    else if (type == long.class) {
                        field.setLong(null, parseLongSafe(raw, field.getLong(null)));
                    }
                    else if (type == float.class) {
                        try {
                            field.setFloat(null, Float.parseFloat(raw));
                        }
                        catch (NumberFormatException ignored) {
                            ;;
                        }
                    }
                    else if (type == double.class) {
                        try {
                            field.setDouble(null, Double.parseDouble(raw));
                        }
                        catch (NumberFormatException ignored) {
                            ;;
                        }

                    }

                }

            }
            catch (Exception ignored) {
                ;;
            }

        }

        boolean resetToDefault() {
            if (defaultValue == null) {
                return false;
            }

            final boolean changed = canReset();
            try {
                if (widget instanceof ToggleButton toggleButton && defaultValue instanceof Boolean bool) {
                    toggleButton.setToggled(bool);
                }
                else if (widget instanceof ConfigSlider configSlider && defaultValue instanceof Number number) {
                    configSlider.setValueRaw(number.doubleValue());
                }
                else if (widget instanceof EnumCycleButton enumCycleButton && defaultValue instanceof Enum<?> enumType) {
                    enumCycleButton.setCurrent(enumType);
                }
                else if (widget instanceof SelectionButton selection) {
                    selection.setFromField(defaultValue);
                }
                else if (widget instanceof EditBox box) {
                    box.setValue(String.valueOf(defaultValue));
                }

            }
            catch (Exception ignored) {
                ;;
            }

            return changed;
        }

        void render(GuiGraphics graphics, float topF, int left, int width, int mx, int my, boolean hovered, float appear, long now, float partialTick) {
            final Font font = Minecraft.getInstance().font;
            final int lineHeight = font.lineHeight;

            if (!hovered) {
                hoverStart = 0;
            }

            hoverAnim = approach(hoverAnim, hovered ? 1f : 0f, hovered ? 18f : 8f);
            modifiedAnim = approach(modifiedAnim, isModified() ? 1f : 0f, 10f);
            resetAnim = approach(resetAnim, hovered && canReset() ? 1f : 0f, hovered ? 16f : 10f);
            final float e = smooth(hoverAnim);

            final int top = Mth.floor(topF);

            beginFade(graphics, appear);
            graphics.pose().pushPose();
            graphics.pose().translate(0f, (topF - top) + (1f - appear) * 6f, 0f);

            // Short accent wash after the row is reset
            float flash = 0f;
            if (flashAt > 0L && now >= flashAt) {
                flash = 1f - Mth.clamp((now - flashAt) / 700f, 0f, 1f);
                flash *= flash;
                if (flash <= 0f) {
                    flashAt = 0L;
                }

            }

            final float open = expansion();
            rounded(graphics, left, top, left + width, top + fullHeight(), opaque(mixRgb(mixRgb(ROW, ROW_HOVER, Math.max(e, open)), accent, 0.2f * flash)), 2);

            final int textAlpha = 0xFF;
            final int contentH = lineHeight + (descriptionLines > 0 ? 2 + descriptionLines * lineHeight : 0);
            final int contentTop = top + (height - contentH) / 2 + 1;
            final int labelX = left + TEXT_INSET;

            if (textAlpha >= 0x08) {
                // Edited values take the accent until they're saved or put back
                final int labelColor = mixRgb(mixRgb(TEXT_LABEL, 0xFFFFFF, e), accent, modifiedAnim);
                graphics.drawString(font, label, labelX, contentTop, withAlpha(labelColor, textAlpha), false);

                if (meta.requiresRestart()) {
                    ICON_RESTART.draw(graphics, labelX + font.width(label) + 5, contentTop - 1, withAlpha(WARNING, textAlpha));
                }

                int commentY = contentTop + lineHeight + 2;
                for (FormattedCharSequence line : descriptionCache) {
                    graphics.drawString(font, line, labelX, commentY, withAlpha(mixRgb(0x66666D, 0x7E7E86, e), textAlpha), false);
                    commentY += lineHeight;
                }

            }

            final int right = left + width - 8;
            final int restingX = right - restingWidgetWidth();
            final int restingY = top + (height - widget.getHeight()) / 2;

            if (open > 0f) {
                final int x = Math.round(Mth.lerp(open, restingX, labelX - StyledEditBox.TEXT_PADDING));
                widget.setX(x);
                widget.setY(Math.round(Mth.lerp(open, restingY, top + height - 3)));
                widget.setWidth(right - x);
            }
            else {
                widget.setX(restingX);
                widget.setY(restingY);
                widget.setWidth(restingWidgetWidth());
            }

            if (resetAnim > 0.01f) {
                final float shown = smooth(resetAnim);
                resetX = Math.round(Mth.lerp(open, restingX - RESET_SIZE - 4, right - RESET_SIZE)) + Math.round((1f - shown) * 6f);
                resetY = top + (height - RESET_SIZE) / 2;

                final boolean resetHovered = mx >= resetX && mx < resetX + RESET_SIZE && my >= resetY && my < resetY + RESET_SIZE;
                resetHoverAnim = approach(resetHoverAnim, resetHovered ? 1f : 0f, 18f);

                final int alpha = (int) (0xFF * shown);
                frame(graphics, resetX, resetY, resetX + RESET_SIZE, resetY + RESET_SIZE, withAlpha(mixRgb(FIELD, FIELD_HOVER, resetHoverAnim), alpha), withAlpha(mixRgb(FIELD_BORDER, FIELD_BORDER_HOVER, resetHoverAnim), alpha), 1);
                ICON_RESET.draw(graphics, resetX + (RESET_SIZE - ICON_RESET.width) / 2, resetY + (RESET_SIZE - ICON_RESET.height) / 2, withAlpha(mixRgb(0x86868D, 0xFFFFFF, resetHoverAnim), alpha));
            }

            widget.render(graphics, mx, my, partialTick);

            graphics.pose().popPose();
            endFade(graphics, appear);
        }

        boolean clickedReset(double mx, double my, int button) {
            if (button != 0 || resetAnim < 0.5f) {
                return false;
            }

            if (mx >= resetX && mx < resetX + RESET_SIZE && my >= resetY && my < resetY + RESET_SIZE) {
                if (resetToDefault()) {
                    flashAt = Util.getMillis();
                }

                playClickSound();
                return true;
            }

            return false;
        }

        boolean mouseClicked(double mx, double my, int button) {
            return widget.mouseClicked(mx, my, button);
        }

        boolean mouseReleased(double mx, double my, int button) {
            return widget.mouseReleased(mx, my, button);
        }

        boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            return widget.mouseDragged(mx, my, button, dx, dy);
        }

        boolean keyPressed(int key, int s, int m) {
            return widget.keyPressed(key, s, m);
        }

        boolean charTyped(char c, int m) {
            return widget.charTyped(c, m);
        }

        private static int parseIntSafe(String safe, int fallback) {
            try {
                safe = safe.trim();
                if (safe.startsWith("#")) {
                    safe = "0x" + safe.substring(1);
                }

                if (safe.startsWith("0x") || safe.startsWith("0X")) {
                    return Integer.decode(safe);
                }

                return Integer.parseInt(safe);
            }
            catch (Exception exception) {
                return fallback;
            }

        }

        private static long parseLongSafe(String safe, long fallback) {
            try {
                safe = safe.trim();
                if (safe.startsWith("#")) {
                    safe = "0x" + safe.substring(1);
                }

                if (safe.startsWith("0x") || safe.startsWith("0X")) {
                    return Long.decode(safe);
                }

                return Long.parseLong(safe);
            }
            catch (Exception exception) {
                return fallback;
            }

        }

    }

    static class SelectionButton extends AbstractWidget {

        final boolean multiple;
        final Class<?> elementType;

        final List<Object> choices;
        final boolean registryIds;
        final ConfigEntry meta;
        final int accent;

        private List<Object> values = new ArrayList<>();
        private float hoverAnim = 0f;
        private float openAnim = 0f;

        SelectionButton(int width, boolean multiple, Class<?> elementType, List<Object> choices, boolean registryIds, ConfigEntry meta, int accent) {
            super(0, 0, width, 18, Component.empty());
            this.multiple = multiple;
            this.elementType = elementType;
            this.choices = choices;
            this.registryIds = registryIds;
            this.meta = meta;
            this.accent = accent;
        }

        List<Object> values() {
            return values;
        }

        void setValues(List<Object> values) {
            this.values = new ArrayList<>(values);
        }

        void setFromField(Object value) {
            values = new ArrayList<>();
            if (value instanceof List<?> list) {
                values.addAll(list);
            }
            else if (value != null && !(value instanceof String text && text.isEmpty())) {
                values.add(value);
            }

        }

        Object single() {
            return values.isEmpty() ? (elementType == String.class ? "" : null) : values.get(0);
        }

        Object fieldValue() {
            if (multiple && values.size() < minSize()) {
                return null;
            }

            return multiple ? new ArrayList<>(values) : single();
        }

        int maxSize() {
            return multiple ? meta.maxSize() : 1;
        }

        int minSize() {
            return multiple ? meta.minSize() : 0;
        }

        String label(Object value) {
            return displayLabel(value, registryIds);
        }

        @Override
        public void onClick(double mx, double my) {
            showPopup(choices != null ? new ConfigPopup.Select(this) : new ConfigPopup.ListEditor(this));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            final Font font = Minecraft.getInstance().font;
            final int x0 = getX();
            final int y0 = getY();
            final int x1 = x0 + getWidth();
            final int y1 = y0 + getHeight();

            final boolean open = isPopupOpenFor(this);
            hoverAnim = approach(hoverAnim, isHovered() ? 1f : 0f, isHovered() ? 16f : 8f);
            openAnim = approach(openAnim, open ? 1f : 0f, 14f);
            final float e = Math.max(smooth(hoverAnim), openAnim);

            frame(graphics, x0, y0, x1, y1, opaque(mixRgb(FIELD, FIELD_HOVER, e)), opaque(mixRgb(mixRgb(FIELD_BORDER, FIELD_BORDER_HOVER, e), accent, openAnim)), 1);

            final Icon chevron = openAnim > 0.5f ? ICON_CHEVRON_UP : ICON_CHEVRON_DOWN;
            final int chevronX = x1 - 6 - chevron.width;
            chevron.draw(graphics, chevronX, y0 + (getHeight() - chevron.height) / 2, opaque(mixRgb(0x6A6A72, 0xC8C8CE, e)));

            final int textX = x0 + 6;
            final int textY = y0 + (getHeight() - font.lineHeight) / 2 + 1;
            final int maxWidth = chevronX - 5 - textX;

            if (values.isEmpty() || (values.size() == 1 && "".equals(values.get(0)))) {
                graphics.drawString(font, I18n.get("knightlib.config.empty_value"), textX, textY, opaque(TEXT_FAINT), false);
            }
            else {
                graphics.drawString(font, summary(font, maxWidth), textX, textY, opaque(mixRgb(TEXT_LABEL, 0xFFFFFF, e)), false);
            }

        }

        private String summary(Font font, int maxWidth) {
            if (values.size() == 1) {
                return ellipsize(font, label(values.get(0)), maxWidth);
            }

            final StringBuilder text = new StringBuilder();
            for (int i = 0; i < values.size(); i++) {
                final String next = (i == 0 ? "" : ", ") + label(values.get(i));
                final int remaining = values.size() - i - 1;
                final String suffix = remaining > 0 ? " +" + remaining : "";

                if (font.width(text + next + suffix) > maxWidth) {
                    if (i == 0) {
                        final String more = " +" + (values.size() - 1);
                        return ellipsize(font, label(values.get(0)), maxWidth - font.width(more)) + more;
                    }

                    return text + " +" + (values.size() - i);
                }

                text.append(next);
            }

            return text.toString();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            ;;
        }

    }

    static class RecordButton extends SelectionButton {

        final Class<?> recordType;
        final RecordComponent[] components;

        RecordButton(int width, Class<?> recordType, ConfigEntry meta, int accent) {
            super(width, true, Object.class, null, false, meta, accent);
            this.recordType = recordType;
            this.components = recordType.getRecordComponents();
        }

        @Override
        void setFromField(Object value) {
            setValues(value instanceof Record record ? ConfigManager.recordValues(record) : List.of());
        }

        @Override
        Object fieldValue() {
            return values().size() == components.length ? ConfigManager.buildRecord(recordType, values()) : null;
        }

        @Override
        public void onClick(double mx, double my) {
            showPopup(new ConfigPopup.RecordEditor(this));
        }

    }

    static class SearchBox extends EditBox {

        private float focusAnim = 0f;
        private float hoverAnim = 0f;
        private float clearHoverAnim = 0f;
        private final int accent;

        private static final int TEXT_PADDING = 18;

        SearchBox(Font font, int x, int y, int width, int height, int accent) {
            super(font, x, y, width, height, Component.literal("Search"));
            this.accent = accent;
            setBordered(false);
            setTextColor(0xFFDADADF);
            setTextColorUneditable(0xFF55555B);
            setMaxLength(128);
            setHint(Component.translatable("knightlib.config.search"));
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            // Clear zone at the right edge when there is text
            if (button == 0 && !getValue().isEmpty()) {
                final int zoneX = getX() + getWidth() - 14;
                if (mx >= zoneX && mx < getX() + getWidth() && my >= getY() && my < getY() + getHeight()) {
                    setValue("");
                    playClickSound();
                    return true;
                }

            }

            return super.mouseClicked(mx, my, button);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            final int x0 = getX();
            final int y0 = getY();
            final int width = getWidth();
            final int height = getHeight();
            final int x1 = x0 + width;
            final int y1 = y0 + height;

            final boolean hovered = mx >= x0 && mx < x1 && my >= y0 && my < y1;
            hoverAnim = approach(hoverAnim, hovered ? 1f : 0f, hovered ? 16f : 8f);
            focusAnim = approach(focusAnim, isFocused() ? 1f : 0f, 14f);

            frame(graphics, x0, y0, x1, y1, opaque(mixRgb(INPUT, INPUT_HOVER, Math.max(hoverAnim, focusAnim))), opaque(mixRgb(mixRgb(INPUT_BORDER, FIELD_BORDER_HOVER, hoverAnim), accent, focusAnim)), 1);

            ICON_SEARCH.draw(graphics, x0 + 5, y0 + (height - ICON_SEARCH.height) / 2, opaque(mixRgb(0x55555C, 0xBDBDC4, Math.max(focusAnim, hoverAnim * 0.4f))));

            if (!getValue().isEmpty()) {
                final boolean clearHovered = mx >= x1 - 14 && mx < x1 && my >= y0 && my < y1;
                clearHoverAnim = approach(clearHoverAnim, clearHovered ? 1f : 0f, 18f);
                ICON_CLEAR.draw(graphics, x1 - 10, y0 + (height - ICON_CLEAR.height) / 2, opaque(mixRgb(0x6A6A72, 0xFFFFFF, clearHoverAnim)));
            }

            // Renders text shifted past the icon
            final int lineHeight = Minecraft.getInstance().font.lineHeight;
            final int offsetY = (height - lineHeight) / 2 + 1;

            graphics.pose().pushPose();

            graphics.pose().translate(TEXT_PADDING, offsetY, 0);

            super.renderWidget(graphics, mx - TEXT_PADDING, my - offsetY, partialTick);

            graphics.pose().popPose();
        }

    }

    static class ToggleButton extends AbstractWidget {

        private boolean toggled;
        private float hoverAnim = 0f;
        private final Spring knob = new Spring();
        private final int accent;

        private static final int KNOB = 10;
        private static final int PAD = 3;

        ToggleButton(int x, int y, int width, int height, boolean initial, int accent) {
            super(x, y, width, height, Component.empty());
            this.toggled = initial;
            this.accent = accent;
            this.knob.snap(initial ? 1f : 0f);
        }

        boolean isToggled() {
            return toggled;
        }

        void setToggled(boolean toggled) {
            this.toggled = toggled;
        }

        @Override
        public void onClick(double mx, double my) {
            toggled = !toggled;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            final int x0 = getX();
            final int y0 = getY();
            final int width = getWidth();
            final int height = getHeight();

            hoverAnim = approach(hoverAnim, isHovered() ? 1f : 0f, isHovered() ? 16f : 8f);

            final float p = knob.update(toggled ? 1f : 0f, 260f, 0.55f);
            final float on = Mth.clamp(p, 0f, 1f);

            final int offTrack = mixRgb(FIELD, FIELD_HOVER, hoverAnim);
            final int offBorder = mixRgb(FIELD_BORDER, FIELD_BORDER_HOVER, hoverAnim);
            final int onTrack = mixRgb(mixRgb(ROW, accent, 0.55f), accent, 0.12f * hoverAnim);
            final int onBorder = mixRgb(onTrack, accent, 0.55f);
            frame(graphics, x0, y0, x0 + width, y0 + height, opaque(mixRgb(offTrack, onTrack, on)), opaque(mixRgb(offBorder, onBorder, on)), 1);

            final int travel = width - PAD * 2 - KNOB;
            final float pressed = Math.abs(p - on) * travel;
            final float stretch = pressed > 0f ? 0f : Math.min(3f, Math.abs(knob.velocity) * 0.3f);
            final float knobWidth = KNOB + stretch - Math.min(3f, pressed * 1.5f);

            final boolean anchorRight = p > 1f || (p >= 0f && knob.velocity > 0f);
            final float restX = x0 + PAD + on * travel;
            final float knobX = anchorRight ? restX + KNOB - knobWidth : restX;

            final int baseX = Mth.floor(knobX);
            graphics.pose().pushPose();
            graphics.pose().translate(knobX - baseX, 0f, 0f);
            rounded(graphics, baseX, y0 + PAD, baseX + Math.round(knobWidth), y0 + height - PAD, opaque(mixRgb(0x8E8E96, 0xFFFFFF, on)), 1);
            graphics.pose().popPose();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            ;;
        }

    }

    static class ConfigSlider extends AbstractWidget {

        private final double min;
        private final double max;
        private double value;
        private final boolean intMode;
        private final int accent;
        private boolean dragging = false;
        private float activeAnim = 0f;
        private float valueHoverAnim = 0f;
        private float knobGrow = 0f;
        private float shownFraction = Float.NaN;
        private final int valueWidth;

        private boolean editing = false;
        private String editBuffer = "";

        private static final int TRACK_LENGTH = 104;
        private static final int TRACK_GAP = 7;
        private static final int KNOB_WIDTH = 8;
        private static final int[] VALUE_BOX_SIZES = {31, 37, 49, 64};

        ConfigSlider(int x, int y, int w, int h, double min, double max, double value, boolean intMode, int accent) {
            super(x, y, w, h, Component.empty());
            this.min = min;
            this.max = max;
            this.value = Math.max(min, Math.min(max, value));
            this.intMode = intMode;
            this.accent = accent;
            this.valueWidth = valueBoxWidth(min, max, intMode);
        }

        static int preferredWidth(double min, double max, boolean intMode) {
            return TRACK_LENGTH + TRACK_GAP + valueBoxWidth(min, max, intMode);
        }

        private static int valueBoxWidth(double min, double max, boolean intMode) {
            final Font font = Minecraft.getInstance().font;
            final int widest = Math.max(font.width(formatFixed(min, intMode)), font.width(formatFixed(max, intMode)));
            for (int size : VALUE_BOX_SIZES) {
                if (widest + 10 <= size) {
                    return size;
                }

            }

            return VALUE_BOX_SIZES[VALUE_BOX_SIZES.length - 1];
        }

        private static String formatFixed(double value, boolean intMode) {
            return intMode ? String.valueOf(Math.round(value)) : String.format(Locale.ROOT, "%.3f", value);
        }

        private int trackLeft() {
            return getX() + KNOB_WIDTH / 2;
        }

        private int trackRight() {
            return getX() + getWidth() - valueWidth - TRACK_GAP - KNOB_WIDTH / 2;
        }

        private int valueLeft() {
            return getX() + getWidth() - valueWidth;
        }

        double getValueRaw() {
            return intMode ? Math.round(value) : value;
        }

        void setValueRaw(double value) {
            this.value = Math.max(min, Math.min(max, value));
        }

        private double frac() {
            return max <= min ? 0 : (value - min) / (max - min);
        }

        private void fromMouse(double mx) {
            double maxed = Math.max(0, Math.min(1, (mx - trackLeft()) / (double) Math.max(1, trackRight() - trackLeft())));
            value = min + maxed * (max - min);
            if (intMode) {
                value = Math.round(value);
            }

        }

        @Override
        public void onClick(double mx, double my) {
            if (editing) {
                commitEdit();
                return;
            }

            if (mx >= valueLeft() || (Screen.hasControlDown() && Screen.hasShiftDown())) {
                editing = true;
                editBuffer = formatCurrent();
                return;
            }

            dragging = true;
            fromMouse(mx);
        }

        @Override
        protected void onDrag(double mx, double my, double dx, double dy) {
            if (dragging) {
                fromMouse(mx);
            }

        }

        @Override
        public void onRelease(double mx, double my) {
            dragging = false;
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);

            if (!focused && editing) {
                commitEdit();
            }

        }

        @Override
        public boolean keyPressed(int key, int s, int m) {
            if (!editing) {
                return false;
            }

            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                commitEdit();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                editing = false;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!editBuffer.isEmpty()) {
                    editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
                }

                return true;
            }

            return false;
        }

        @Override
        public boolean charTyped(char c, int m) {
            if (!editing) {
                return false;
            }

            if (Character.isDigit(c) || c == '-' || (!intMode && (c == '.' || c == ','))) {
                editBuffer += (c == ',' ? '.' : c);
            }

            return true;
        }

        private void commitEdit() {
            editing = false;
            try {
                setValueRaw(Double.parseDouble(editBuffer.trim()));
                if (intMode) {
                    value = Math.round(value);
                }

            }
            catch (NumberFormatException ignored) {
                ;;
            }

        }

        private String formatCurrent() {
            if (intMode) {
                return String.valueOf((long) getValueRaw());
            }

            final double raw = getValueRaw();
            if (raw == Math.floor(raw) && !Double.isInfinite(raw)) {
                return String.valueOf((long) raw);
            }

            return String.format(Locale.ROOT, "%.6f", raw).replaceAll("0+$", "").replaceAll("\\.$", "");
        }

        private String formatShown() {
            if (intMode) {
                return String.valueOf((long) getValueRaw());
            }

            return String.format(Locale.ROOT, "%.3f", getValueRaw()).replaceAll("0+$", "").replaceAll("\\.$", "");
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            final int x0 = getX();
            final int y0 = getY();
            final int width = getWidth();
            final int height = getHeight();
            final int x1 = x0 + width;
            final int y1 = y0 + height;
            final Font font = Minecraft.getInstance().font;

            final int valueX = valueLeft();
            final boolean onTrack = isHovered() && mx < valueX - TRACK_GAP / 2;
            final boolean onValue = isHovered() && mx >= valueX;

            activeAnim = approach(activeAnim, onTrack || dragging ? 1f : 0f, onTrack || dragging ? 16f : 8f);
            valueHoverAnim = approach(valueHoverAnim, onValue || editing ? 1f : 0f, 16f);

            // Value box on the right, also where exact values are typed
            final int valueBorder = editing ? (accent & 0x00FFFFFF) : mixRgb(INPUT_BORDER, FIELD_BORDER_HOVER, valueHoverAnim);
            frame(graphics, valueX, y0, x1, y1, opaque(mixRgb(INPUT, INPUT_HOVER, valueHoverAnim)), opaque(valueBorder), 1);

            final String text = editing ? editBuffer + "_" : formatShown();
            final String drawn = editing && (Util.getMillis() / 400L) % 2 != 0 ? editBuffer : text;
            final int textColor = editing ? 0xFFFFFF : mixRgb(0xC8C8CE, 0xFFFFFF, Math.max(activeAnim, valueHoverAnim));
            KnightLibConfigScreen.enableScissor(graphics, valueX + 1, y0 + 1, x1 - 1, y1 - 1);
            graphics.drawString(font, drawn, valueX + (valueWidth - font.width(text) + 1) / 2, y0 + (height - font.lineHeight) / 2 + 1, opaque(textColor), false);
            graphics.disableScissor();

            final float target = (float) frac();
            shownFraction = Float.isNaN(shownFraction) ? target : approach(shownFraction, target, dragging ? 45f : 14f);

            final int trackL = trackLeft();
            final int trackR = trackRight();
            final int grooveRight = trackR + KNOB_WIDTH / 2;
            final int midY = y0 + height / 2;

            frame(graphics, x0, midY - 2, grooveRight, midY + 2, opaque(INPUT), opaque(mixRgb(INPUT_BORDER, FIELD_BORDER_HOVER, activeAnim)), 1);

            final float knobCenter = trackL + shownFraction * (trackR - trackL);
            final int fillRight = Math.round(knobCenter);
            final int fillColor = mixRgb(ROW, accent, 0.72f + 0.28f * activeAnim);
            if (fillRight > x0 + 1) {
                rounded(graphics, x0, midY - 2, fillRight, midY + 2, opaque(fillColor), 1);
            }

            final long steps = intMode ? Math.round(max - min) : 0L;
            if (steps >= 2 && steps <= 12) {
                for (int i = 1; i < steps; i++) {
                    final int tickX = Math.round(trackL + (float) i / steps * (trackR - trackL));
                    final int tickColor = tickX < fillRight ? mixRgb(fillColor, BACKGROUND, 0.35f) : mixRgb(INPUT_BORDER, FIELD_BORDER_HOVER, activeAnim);
                    graphics.fill(tickX, midY - 1, tickX + 1, midY + 1, opaque(tickColor));
                }

            }

            // A squared block that widens a little under the cursor and grows taller while held
            knobGrow = approach(knobGrow, dragging ? 1f : (onTrack ? 0.5f : 0f), 18f);
            final int knobWidth = 8 + Math.round(2f * Math.min(1f, knobGrow * 2f));
            final int knobHeight = 12 + Math.round(2f * Math.max(0f, knobGrow * 2f - 1f));

            final float knobX = knobCenter - knobWidth / 2f;
            final int baseX = Mth.floor(knobX);
            final int knobTop = y0 + (height - knobHeight) / 2;

            graphics.pose().pushPose();
            graphics.pose().translate(knobX - baseX, 0f, 0f);
            final int knobFill = mixRgb(0xB0B0B7, 0xFFFFFF, Math.max(activeAnim * 0.6f, knobGrow * 2f - 1f));
            final int knobBorder = mixRgb(mixRgb(0x3E3E46, 0x56565F, activeAnim), accent, Math.max(0f, knobGrow * 2f - 1f));
            frame(graphics, baseX, knobTop, baseX + knobWidth, knobTop + knobHeight, opaque(knobFill), opaque(knobBorder), 1);
            graphics.pose().popPose();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            ;;
        }

    }

    static class EnumCycleButton extends AbstractWidget {

        private final Enum<?>[] values;
        private final String[] labels;
        private int idx;
        private float hoverAnim = 0f;
        private final int accent;

        private int previous = -1;
        private int direction = 1;
        private long changedAt = 0L;

        private static final int SLIDE = 12;

        EnumCycleButton(int x, int y, int width, int height, Enum<?>[] values, Enum<?> current, int accent) {
            super(x, y, width, height, Component.empty());
            this.values = values;
            this.accent = accent;
            this.labels = new String[values.length];
            this.idx = 0;
            for (int i = 0; i < values.length; i++) {
                labels[i] = prettify(values[i].name());
                if (values[i] == current) {
                    idx = i;
                }

            }

        }

        Enum<?> getCurrent() {
            return values[idx];
        }

        void setCurrent(Enum<?> value) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] == value) {
                    if (i != idx) {
                        moveTo(i, i > idx ? 1 : -1);
                    }

                    return;
                }

            }

        }

        private void moveTo(int index, int direction) {
            this.previous = idx;
            this.idx = index;
            this.direction = direction;
            this.changedAt = Util.getMillis();
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!active || !visible || (button != 0 && button != 1) || !clicked(mx, my)) {
                return false;
            }

            playDownSound(Minecraft.getInstance().getSoundManager());
            final int step = button == 1 ? -1 : 1;
            moveTo(Math.floorMod(idx + step, values.length), step);

            return true;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            final int x0 = getX();
            final int y0 = getY();
            final int width = getWidth();
            final int height = getHeight();
            final int x1 = x0 + width;
            final int y1 = y0 + height;
            final Font font = Minecraft.getInstance().font;

            hoverAnim = approach(hoverAnim, isHovered() ? 1f : 0f, isHovered() ? 16f : 8f);
            final float e = smooth(hoverAnim);

            frame(graphics, x0, y0, x1, y1, opaque(mixRgb(FIELD, FIELD_HOVER, e)), opaque(mixRgb(FIELD_BORDER, FIELD_BORDER_HOVER, e)), 1);

            final int chevronColor = opaque(mixRgb(0x56565D, 0xB0B0B7, e));
            final int chevronY = y0 + (height - ICON_CHEVRON_LEFT.height) / 2;
            ICON_CHEVRON_LEFT.draw(graphics, x0 + 5 - Math.round(e), chevronY, chevronColor);
            ICON_CHEVRON_RIGHT.draw(graphics, x1 - 5 - ICON_CHEVRON_RIGHT.width + Math.round(e), chevronY, chevronColor);

            final int textY = y0 + (height - font.lineHeight) / 2 + 1;
            final int center = x0 + width / 2;
            final int textColor = mixRgb(0xD0D0D5, 0xFFFFFF, e);

            final float t = ease((Util.getMillis() - changedAt) / 240f);
            if (previous >= 0 && t < 1f) {
                KnightLibConfigScreen.enableScissor(graphics, x0 + 11, y0, x1 - 11, y1);
                drawSliding(graphics, font, labels[previous], center, textY, -direction * t * SLIDE, 1f - t, textColor);
                drawSliding(graphics, font, labels[idx], center, textY, direction * (1f - t) * SLIDE, t, textColor);
                graphics.disableScissor();
            }
            else {
                graphics.drawString(font, labels[idx], center - font.width(labels[idx]) / 2, textY, opaque(textColor), false);
            }

        }

        private static void drawSliding(GuiGraphics graphics, Font font, String text, int center, int y, float offset, float alpha, int color) {
            final int a = (int) (0xFF * alpha);
            if (a < 0x08) {
                return;
            }

            graphics.pose().pushPose();
            graphics.pose().translate(offset, 0f, 0f);
            graphics.drawString(font, text, center - font.width(text) / 2, y, withAlpha(color, a), false);
            graphics.pose().popPose();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            ;;
        }

    }

    static class StyledEditBox extends EditBox {

        static final int TEXT_PADDING = 5;

        private float focusAnim = 0f;
        private float hoverAnim = 0f;
        private float expandAnim = 0f;
        private final int baseWidth;
        private final boolean expandable;
        private final int accent;
        private Predicate<String> validator = raw -> true;

        StyledEditBox(Font font, int x, int y, int width, int height, Component message, int accent, boolean expandable) {
            super(font, x, y, width, height, message);
            this.baseWidth = width;
            this.expandable = expandable;
            this.accent = accent;
            setBordered(false);
            setTextColor(0xFFD6D6DA);
            setTextColorUneditable(0xFF66666C);
            setMaxLength(512);
        }

        void setValidator(Predicate<String> validator) {
            this.validator = validator;
        }

        int baseWidth() {
            return baseWidth;
        }

        void updateExpand() {
            expandAnim = approach(expandAnim, expandable && isFocused() ? 1f : 0f, 14f);
        }

        float expandProgress() {
            return smooth(expandAnim);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            final int x0 = getX();
            final int y0 = getY();
            final int x1 = x0 + getWidth();
            final int y1 = y0 + getHeight();

            final boolean hovered = mx >= x0 && mx < x1 && my >= y0 && my < y1;
            hoverAnim = approach(hoverAnim, hovered ? 1f : 0f, hovered ? 16f : 8f);
            focusAnim = approach(focusAnim, isFocused() ? 1f : 0f, 14f);

            final boolean valid = validator.test(getValue());

            final int fill = valid ? mixRgb(INPUT, INPUT_HOVER, hoverAnim) : 0x231314;
            final int border = valid ? mixRgb(mixRgb(INPUT_BORDER, FIELD_BORDER_HOVER, hoverAnim), accent, focusAnim) : ERROR;
            frame(graphics, x0, y0, x1, y1, opaque(fill), opaque(border), 1);

            int lineHeight = Minecraft.getInstance().font.lineHeight;
            int offsetY = (this.height - lineHeight) / 2 + 1;
            int paddingX = TEXT_PADDING;

            KnightLibConfigScreen.enableScissor(graphics, x0 + 1, y0 + 1, x1 - 1, y1 - 1);

            graphics.pose().pushPose();
            graphics.pose().translate(paddingX, offsetY, 0);
            super.renderWidget(graphics, mx - paddingX, my - offsetY, partialTick);
            graphics.pose().popPose();

            graphics.disableScissor();
        }

    }

    static class FlatButton extends Button {

        private float hoverAnim = 0f;
        private float pressAnim = 0f;
        private final int accent;
        private final boolean primary;

        private final long createdAt = Util.getMillis();
        private int entranceDelay = 0;

        FlatButton(int x, int y, int width, int height, Component message, OnPress onPress, int accent) {
            this(x, y, width, height, message, onPress, accent, false);
        }

        FlatButton(int x, int y, int width, int height, Component message, OnPress onPress, int accent, boolean primary) {
            super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
            this.accent = accent;
            this.primary = primary;
        }

        FlatButton withEntranceDelay(int delayMs) {
            this.entranceDelay = delayMs;
            return this;
        }

        @Override
        public void onClick(double mx, double my) {
            pressAnim = 1f;
            super.onClick(mx, my);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            final float entrance = ease((Util.getMillis() - createdAt - entranceDelay) / 340f);
            if (entrance < 0.02f) {
                return;
            }

            final boolean hovering = isHovered() && active;
            hoverAnim = approach(hoverAnim, hovering ? 1f : 0f, hovering ? 18f : 8f);
            pressAnim = approach(pressAnim, 0f, 9f);

            final int x0 = getX();
            final int y0 = getY();
            final int x1 = x0 + getWidth();
            final int y1 = y0 + getHeight();

            beginFade(graphics, entrance);

            graphics.pose().pushPose();

            graphics.pose().translate(0f, (1f - entrance) * 5f + pressAnim, 0f);

            final int face;
            final int border;
            if (primary) {
                face = mixRgb(mixRgb(mixRgb(SURFACE, accent, 0.5f), accent, 0.2f * hoverAnim), BACKGROUND, 0.25f * pressAnim);
                border = mixRgb(face, accent, 0.35f);
            }
            else {
                face = mixRgb(mixRgb(FIELD, FIELD_HOVER, hoverAnim), BACKGROUND, 0.25f * pressAnim);
                border = mixRgb(FIELD_BORDER, FIELD_BORDER_HOVER, hoverAnim);
            }

            frame(graphics, x0, y0, x1, y1, opaque(face), opaque(border), 1);

            final Font font = Minecraft.getInstance().font;
            final int textColor = !active ? 0x5A5A60 : (primary ? 0xFFFFFF : mixRgb(0xBCBCC2, 0xFFFFFF, hoverAnim));
            graphics.drawString(font, getMessage(), x0 + (getWidth() - font.width(getMessage())) / 2, y0 + (getHeight() - font.lineHeight) / 2 + 1, opaque(textColor), false);

            graphics.pose().popPose();
            endFade(graphics, entrance);
        }

    }

}