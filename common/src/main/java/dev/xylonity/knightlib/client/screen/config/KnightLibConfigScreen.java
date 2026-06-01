package dev.xylonity.knightlib.client.screen.config;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
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
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.util.*;

public class KnightLibConfigScreen extends Screen {

    private final Screen parent;
    private final Class<?> configClass;
    private final AutoConfig meta;
    private final int accent;

    private ConfigPanel panel;
    private SearchBox searchBox;

    private final Map<Field, Object> snapshot = new LinkedHashMap<>();

    private static final int HEADER_HEIGHT = 52;
    private static final int DEFAULT_ACCENT = 0xFF4A9EFF;

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
                snapshot.put(field, field.get(null));
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
        int panelTop = HEADER_HEIGHT + 4;
        int panelBottom = this.height - 36;

        this.panel = new ConfigPanel(16, panelTop, this.width - 32, Math.max(0, panelBottom - panelTop), accent);
        this.addRenderableWidget(this.panel);

        final int searchWidth = Math.min(220, this.width / 3);
        final int searchX = this.panel.getX() + 4;
        final int searchY = HEADER_HEIGHT - 22;

        this.searchBox = new SearchBox(this.font, searchX, searchY, searchWidth, 16, accent);
        this.searchBox.setResponder(q -> { if (this.panel != null) this.panel.applySearch(q); });
        this.addRenderableWidget(this.searchBox);

        for (Field field : configClass.getDeclaredFields()) {
            ConfigEntry entry = field.getAnnotation(ConfigEntry.class);
            if (entry == null) {
                continue;
            }

            field.setAccessible(true);

            this.panel.addEntry(field, entry);
        }

        this.panel.applySearch("");

        int buttonWidth = 90;
        int buttonHeight = 18;
        int gap = 8;
        int totalWidth = buttonWidth * 3 + gap * 2;
        int sx = (this.width - totalWidth) / 2;
        int buttonY = this.height - 26;

        addRenderableWidget(new FlatButton(sx, buttonY, buttonWidth, buttonHeight,
                CommonComponents.GUI_DONE,
                button -> {
                    panel.applyToFields(); ConfigManager.save(configClass); minecraft.setScreen(parent);
                },
                accent));

        addRenderableWidget(new FlatButton(sx + buttonWidth + gap, buttonY, buttonWidth, buttonHeight,
                Component.literal("Cancel"),
                button -> {
                    restoreSnapshot(); minecraft.setScreen(parent);
                },
                accent));

        addRenderableWidget(new FlatButton(sx + (buttonWidth + gap) * 2, buttonY, buttonWidth, buttonHeight,
                Component.literal("Reset All"),
                button -> panel.resetAllToDefault(),
                accent));
    }

    @Override
    public void tick() {
        if (searchBox != null) {
            searchBox.tick();
        }

        if (panel != null) {
            panel.tickPanel();
        }

        super.tick();
    }

    @Override
    public void onClose() {
        panel.applyToFields(); ConfigManager.save(configClass); minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF0E0E0E);

        renderHeader(graphics);
        renderFooterLine(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphics graphics) {
        for (int i = 0; i < HEADER_HEIGHT; i++) {
            int alpha = Math.max(0, 60 - i * 2);
            if (alpha > 0) {
                graphics.fill(0, i, width, i + 1, alpha << 24);
            }

        }

        final String title = resolveTitle(configClass);
        graphics.drawString(font, title, width - 14 - font.width(title), 8, 0xFFFFFFFF, false);

        final String description = meta != null ? meta.description() : "";
        if (!description.isEmpty()) {
            graphics.drawString(font, description, width - 14 - font.width(description), 20, 0xFF888888, false);
        }

    }

    private void renderFooterLine(GuiGraphics graphics) {
        int width = (int) (this.width * 0.75f);
        int x0 = (this.width - width) / 2;

        // Top line (above entries)
        drawFadedHLine(graphics, x0, x0 + width, HEADER_HEIGHT + 2, 0x4D4D4D);

        // Bottom line (above buttons)
        drawFadedHLine(graphics, x0, x0 + width, height - 32, 0x4D4D4D);
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

    static int[] rgb(int argb) {
        return new int[] {
                (argb >> 16) & 0xFF,
                (argb >> 8) & 0xFF,
                argb & 0xFF
        };

    }

    static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    static void drawFadedHLine(GuiGraphics graphics, int x0, int x1, int y, int rgb) {
        Matrix4f matrix = graphics.pose().last().pose();

        final int red = (rgb >> 16) & 0xFF;
        final int green = (rgb >> 8) & 0xFF;
        final int blue = rgb & 0xFF;
        final int cx = (x0 + x1) / 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        final BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        bufferBuilder.vertex(matrix, x0, y + 1, 0).color(red, green, blue, 0).endVertex();
        bufferBuilder.vertex(matrix, cx, y + 1, 0).color(red, green, blue, 255).endVertex();
        bufferBuilder.vertex(matrix, cx, y, 0).color(red, green, blue, 255).endVertex();
        bufferBuilder.vertex(matrix, x0, y, 0).color(red, green, blue, 0).endVertex();
        bufferBuilder.vertex(matrix, cx, y + 1, 0).color(red, green, blue, 255).endVertex();
        bufferBuilder.vertex(matrix, x1, y + 1, 0).color(red, green, blue, 0).endVertex();
        bufferBuilder.vertex(matrix, x1, y, 0).color(red, green, blue, 0).endVertex();
        bufferBuilder.vertex(matrix, cx, y, 0).color(red, green, blue, 255).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    static void drawHoverParticles(GuiGraphics graphics, int left, int top, int height, float anim, int accent, int seed) {
        if (anim <= 0.05f) {
            return;
        }

        long time = Util.getMillis();
        int[] ac = rgb(accent);
        int count = 6;
        float maxDrift = 55f;

        for (int i = 0; i < count; i++) {
            long period = 1400L + (i * 200L);
            long offset = Math.floorMod((long) seed * 137L + i * 311L, period);
            float phase = Math.floorMod(time + offset, period) / (float) period;

            float mx = left + 3 + phase * maxDrift;
            float yCenter = top + height * 0.5f;
            float yOff = (float) Math.sin(phase * Math.PI * 2.0 + i * 1.7) * (height * 0.3f);
            float my = yCenter + yOff;

            float lifeAlpha = (float) Math.sin(phase * Math.PI);
            float alpha = lifeAlpha * anim * 0.75f;
            if (alpha < 0.02f) {
                continue;
            }

            int hexAlpha = (int) (alpha * 255);
            int size = phase < 0.3f ? 2 : 1;
            graphics.fill((int) mx, (int) my, (int) mx + size, (int) my + size, (hexAlpha << 24) | (ac[0] << 16) | (ac[1] << 8) | ac[2]);
        }

    }

    static void drawBarGlow(GuiGraphics graphics, int left, int top, int h, float anim, int accent) {
        if (anim <= 0.05f) {
            return;
        }

        int[] ac = rgb(accent);
        final int glowWidth = 6;
        final int maxAlpha = (int) (0x20 * anim);
        for (int x = 0; x < glowWidth; x++) {
            float time = (float) x / glowWidth;
            int alpha = (int) (maxAlpha * (1f - time * time));
            if (alpha > 0) {
                graphics.fill(left + 3 + x, top, left + 4 + x, top + h, (alpha << 24) | (ac[0] << 16) | (ac[1] << 8) | ac[2]);
            }

        }

    }

    static class ConfigPanel extends AbstractWidget {

        private final List<EntryRow> allRows = new ArrayList<>();
        private final List<EntryRow> visibleRows = new ArrayList<>();

        private double scroll = 0;
        private double targetScroll = 0;
        private boolean draggingScrollbar = false;
        private int dragOffset = 0;
        private EntryRow focused = null;
        final int accent;

        private static final int ROW_GAP = 3;
        private static final int PADDING = 8;
        private static final int CATEGORY_HEIGHT = 18;

        ConfigPanel(int x, int y, int width, int height, int accent) {
            super(x, y, width, height, Component.empty());
            this.accent = accent;
        }

        void tickPanel() {
            for (EntryRow entryRow : visibleRows) {
                if (entryRow.widget instanceof EditBox editBox) {
                    editBox.tick();
                }

            }

        }

        void addEntry(Field field, ConfigEntry configEntry) {
            allRows.add(new EntryRow(field, configEntry, accent));
            visibleRows.add(allRows.get(allRows.size() - 1));

            recalcLayout();
        }

        void applySearch(String query) {
            visibleRows.clear();
            if (query == null || query.isBlank()) {
                visibleRows.addAll(allRows);
            }
            else {
                String content = query.toLowerCase().replace('_', ' ');
                for (EntryRow entryRow : allRows) {
                    if (entryRow.matchesSearch(content)) visibleRows.add(entryRow);
                }

            }

            recalcLayout();

            targetScroll = 0;
            scroll = 0;
        }

        void applyToFields() {
            for (EntryRow entryRow : allRows) {
                entryRow.writeToField();
            }

        }

        void resetAllToDefault() {
            for (EntryRow entryRow : allRows) {
                entryRow.resetToDefault();
            }

        }

        private int contentWidth() {
            return width - 16;
        }

        private int rowLeft() {
            return getX() + PADDING;
        }

        private void recalcLayout() {
            int width = contentWidth();
            for (EntryRow entryRow : visibleRows) {
                entryRow.recalcHeight(width);
            }

            clampScroll();
        }

        private int totalContentHeight() {
            int height = 4;
            String lastCategory = null;
            for (int i = 0; i < visibleRows.size(); i++) {
                if (i > 0) {
                    height += ROW_GAP;
                }

                String category = visibleRows.get(i).meta.category().trim();
                if (!category.isEmpty() && !category.equals(lastCategory)) {
                    height += CATEGORY_HEIGHT;
                }

                lastCategory = category;
                height += visibleRows.get(i).height;
            }

            return height;
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

            if (!draggingScrollbar && scroll != targetScroll) {
                double dragging = targetScroll - scroll;
                scroll = Math.abs(dragging) < 0.5 ? targetScroll : scroll + dragging * 0.25;

                clampScroll();
            }

            final int x = getX();
            final int y = getY();
            final int addX = x + width;
            final int addY = y + height;
            graphics.fill(x, y, addX, addY, 0xFF111111);

            // Inner shadow at top edge
            for (int i = 0; i < 4; i++) {
                graphics.fill(x, y + i, addX, y + i + 1, (Math.max(0, 30 - i * 8)) << 24);
            }

            graphics.enableScissor(x, y, addX, addY);

            final int rowLength = rowLeft();
            final int rowWidth = contentWidth();
            int categoryY = y + 4 - (int) scroll;
            String lastCategory = null;

            for (int i = 0; i < visibleRows.size(); i++) {
                EntryRow row = visibleRows.get(i);
                if (i > 0) {
                    categoryY += ROW_GAP;
                }

                String category = row.meta.category().trim();
                if (!category.isEmpty() && !category.equals(lastCategory)) {
                    if (categoryY + CATEGORY_HEIGHT >= y && categoryY <= addY) {
                        renderCategoryHeader(graphics, category, rowLength, categoryY, rowWidth);
                    }

                    categoryY += CATEGORY_HEIGHT;
                }

                lastCategory = category;

                int rowH = row.height;
                if (categoryY + rowH >= y && categoryY <= addY) {
                    boolean inPanel = mx >= x && mx <= addX && my >= y && my <= addY;
                    boolean hovered = inPanel && mx >= rowLength && mx <= rowLength + rowWidth && my >= categoryY && my <= categoryY + rowH;
                    row.render(graphics, i, categoryY, rowLength, rowWidth, mx, my, hovered, partialTick);
                }

                categoryY += rowH;
            }

            graphics.disableScissor();

            renderScrollbar(graphics);
        }

        private void renderCategoryHeader(GuiGraphics graphics, String category, int x, int y, int width) {
            final Minecraft minecraft = Minecraft.getInstance();
            final String label = prettify(category).toUpperCase();
            final int labelW = minecraft.font.width(label);
            final int textX = x + 12;
            final int lineY = y + 8;
            final int lineEnd = x + width;

            // Small accent dot at the left of the category label
            int[] rgbAccent = rgb(accent);
            graphics.fill(x + 3, lineY - 2, x + 6, lineY + 1, 0xFF000000 | (rgbAccent[0] << 16) | (rgbAccent[1] << 8) | rgbAccent[2]);

            graphics.drawString(minecraft.font, label, textX, y + 4, 0xFF585858, false);

            int lineStart = textX + labelW + 6;
            if (lineStart < lineEnd) {
                graphics.fill(lineStart, lineY, lineEnd, lineY + 1, 0x20FFFFFF);
            }

        }

        private void renderScrollbar(GuiGraphics graphics) {
            if (!scrollable()) {
                return;
            }

            final int contentHeight = totalContentHeight();
            final int newX = getX() + width - 4;
            final int newY = getY();
            final int newHeight = height;
            final double maxSwap = Math.max(1, contentHeight - height);
            final int thumbHeight = Math.max(20, (int) (newHeight * ((double) height / contentHeight)));
            final int thumbY = newY + (int) ((newHeight - thumbHeight) * (scroll / maxSwap));

            graphics.fill(newX, newY, newX + 3, newY + newHeight, 0x10FFFFFF);
            graphics.fill(newX, thumbY, newX + 3, thumbY + thumbHeight, accent);

            graphics.fill(newX - 1, thumbY, newX, thumbY + thumbHeight, withAlpha(accent, 0x15));
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double d) {
            if (!visible || !isIn(mx, my) || !scrollable()) {
                return false;
            }

            draggingScrollbar = false;
            targetScroll -= d * 24.0;

            clampScroll();

            return true;
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!visible || !active || !isIn(mx, my)) {
                return false;
            }

            if (scrollable()) {
                int tx = getX() + width - 6;
                if (mx >= tx) {
                    final int contentHeight = totalContentHeight();
                    final double maxSroll = Math.max(1, contentHeight - height);
                    final int thumbH = Math.max(20, (int) (height * ((double) height / contentHeight)));
                    final int thumbY = getY() + (int) ((height - thumbH) * (scroll / maxSroll));
                    if (my >= thumbY && my <= thumbY + thumbH) {
                        draggingScrollbar = true;
                        dragOffset = (int) my - thumbY;
                    }
                    else {
                        double t = (my - getY()) / (double) height;
                        targetScroll = t * maxSroll;
                        scroll = targetScroll;

                        clampScroll();
                    }

                    return true;
                }

            }

            int contentY = getY() + 4 - (int) scroll;
            String lastCategory = null;
            for (int i = 0; i < visibleRows.size(); i++) {
                final EntryRow row = visibleRows.get(i);
                if (i > 0) {
                    contentY += ROW_GAP;
                }

                final String category = row.meta.category().trim();
                if (!category.isEmpty() && !category.equals(lastCategory)) {
                    contentY += CATEGORY_HEIGHT;
                }

                lastCategory = category;
                if (my >= contentY && my <= contentY + row.height && mx >= rowLeft() && mx <= rowLeft() + contentWidth()) {
                    if (row.mouseClicked(mx, my, button)) {
                        setFocusedRow(row);
                        return true;
                    }

                    clearFocused();

                    return true;
                }

                contentY += row.height;
            }

            clearFocused();

            return false;
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            if (draggingScrollbar && scrollable()) {
                final int contentHeight = totalContentHeight();
                final double maxS = Math.max(1, contentHeight - height);
                final int thumbH = Math.max(20, (int) (height * ((double) height / contentHeight)));
                final int span = height - thumbH;
                if (span <= 0) {
                    return true;
                }

                double maxed = Math.max(0, Math.min(1, ((my - dragOffset - getY()) / span)));

                targetScroll = maxed * maxS;
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
            return (focused != null && focused.keyPressed(key, s, m)) || super.keyPressed(key, s, m);
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

        final Field field;
        final ConfigEntry meta;
        final Component label;
        final String description;
        final AbstractWidget widget;
        final String searchIndex;
        final int accent;

        float hoverAnim = 0f;
        int height = 36;
        int descriptionLines = 0;
        int descriptionMaxWidth = 0;

        EntryRow(Field field, ConfigEntry meta, int accent) {
            this.field = field;
            this.meta = meta;
            this.accent = accent;
            this.label = Component.literal(prettify(field.getName()));
            this.description = meta.comment().trim();
            this.searchIndex = (field.getName() + " " + prettify(field.getName()) + " " + meta.category() + " " + description).toLowerCase().replace('_', ' ');
            this.widget = createWidget();
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

        void recalcHeight(int rowWidth) {
            final Minecraft minecraft = Minecraft.getInstance();
            final int lineHeight = minecraft.font.lineHeight;
            final int avail = rowWidth - widget.getWidth() - 30;

            descriptionMaxWidth = Math.min(avail, (int) (rowWidth * 0.5f));
            descriptionLines = (!description.isEmpty() && descriptionMaxWidth > 30) ? minecraft.font.split(Component.literal(description), descriptionMaxWidth).size() : 0;

            final int contentHeight = lineHeight + (descriptionLines > 0 ? 2 + descriptionLines * lineHeight : 0);
            height = Math.max(Math.max(contentHeight + 10, widget.getHeight() + 10), 28);
        }

        private AbstractWidget createWidget() {
            final Minecraft minecraft = Minecraft.getInstance();
            final Class<?> type = field.getType();
            try {
                if (type == boolean.class) {
                    return new ToggleButton(0, 0, 36, 18, field.getBoolean(null), accent);
                }

                if (meta.slider() && Double.isFinite(meta.min()) && Double.isFinite(meta.max())) {
                    boolean isInt = type == int.class || type == long.class;
                    return new ConfigSlider(0, 0, 120, 18, meta.min(), meta.max(), ((Number) field.get(null)).doubleValue(), isInt, accent);
                }

                if (type.isEnum()) {
                    return new EnumCycleButton(0, 0, 100, 18, (Enum<?>[]) type.getEnumConstants(), (Enum<?>) field.get(null), accent);
                }

                final String value = String.valueOf(field.get(null));
                final EditBox box = new StyledEditBox(minecraft.font, 0, 0, value.length() > 20 ? 200 : 120, 18, Component.empty(), accent);

                box.setMaxLength(512);
                box.setValue(value);

                return box;
            }
            catch (Exception exception) {
                final EditBox box = new StyledEditBox(minecraft.font, 0, 0, 120, 18, Component.empty(), accent);
                box.setValue("");
                return box;
            }

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

        void resetToDefault() {
            Object defaultValue = ConfigManager.getCodeDefault(field);
            if (defaultValue == null) {
                return;
            }
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
                else if (widget instanceof EditBox box) {
                    box.setValue(String.valueOf(defaultValue));
                }

            }
            catch (Exception ignored) {
                ;;
            }

        }

        void render(GuiGraphics graphics, int index, int top, int left, int width, int mx, int my, boolean hovered, float partialTick) {
            final Minecraft minecraft = Minecraft.getInstance();
            final int lineHeight = minecraft.font.lineHeight;

            final float target = hovered ? 1f : 0f;
            final float diff = target - hoverAnim;
            hoverAnim += diff * 0.12f;
            if (Math.abs(diff) < 0.005f) {
                hoverAnim = target;
            }

            float e = KnightLibEasings.SMOOTHSTEP.apply(hoverAnim);

            int baseAlpha = (index % 2 == 0) ? 0x1A : 0x14;
            int extraAlpha1 = (int) (0x20 * e);
            graphics.fill(left, top, left + width, top + height, (Math.min(255, baseAlpha + extraAlpha1) << 24) | 0x1A1A1A);

            // Left accent bar
            final int[] accent = rgb(this.accent);
            final int baseRed = (int) (0x25 + (accent[0] - 0x25) * e);
            final int baseGreen = (int) (0x25 + (accent[1] - 0x25) * e);
            final int baseBlue = (int) (0x25 + (accent[2] - 0x25) * e);
            final int barColor = 0xFF000000 | (baseRed << 16) | (baseGreen << 8) | baseBlue;
            graphics.fill(left, top, left + 2, top + height, barColor);

            // Extra column fades in with alpha
            if (e > 0.01f) {
                final int extraAlpha2 = (int) (0xFF * e);
                final int extraColor = (extraAlpha2 << 24) | (accent[0] << 16) | (accent[1] << 8) | accent[2];
                graphics.fill(left + 2, top, left + 3, top + height, extraColor);

                // Second extra column for full hover
                if (e > 0.5f) {
                    final int extraAlpha3 = (int) (0xFF * (e - 0.5f) * 2f);
                    final int extraColor2 = (extraAlpha3 << 24) | (accent[0] << 16) | (accent[1] << 8) | accent[2];
                    graphics.fill(left + 3, top, left + 4, top + height, extraColor2);
                }

            }

            drawBarGlow(graphics, left, top, height, e, this.accent);
            drawHoverParticles(graphics, left, top, height, e, this.accent, index * 73 + field.getName().hashCode());

            if (e > 0.01f) {
                int gradientWidth = Math.min(70, width / 4);
                int maxAlpha = (int) (0x18 * e);
                for (int x = 0; x < gradientWidth; x++) {
                    final float t = (float) x / gradientWidth;
                    final int alpha = (int) (maxAlpha * (1f - t * t));
                    if (alpha > 0) {
                        graphics.fill(left + x, top, left + x + 1, top + height, (alpha << 24) | (accent[0] << 16) | (accent[1] << 8) | accent[2]);
                    }

                }

            }

            final int contentH = lineHeight + (descriptionLines > 0 ? 2 + descriptionLines * lineHeight : 0);
            final int contentTop = top + (height - contentH) / 2;
            final int labelX = left + 8;

            graphics.drawString(minecraft.font, label, labelX, contentTop, 0xFFE8E8E8, false);

            if (meta.requiresRestart()) {
                int badgeX = labelX + minecraft.font.width(label) + 4;
                graphics.drawString(minecraft.font, "⟳", badgeX, contentTop, 0xFFFF8844, false);
            }

            final int widgetX = left + width - widget.getWidth() - 8;
            final int widgetY = top + (height - widget.getHeight()) / 2;

            widget.setX(widgetX);
            widget.setY(widgetY);

            widget.render(graphics, mx, my, partialTick);

            // Description
            if (descriptionLines > 0 && descriptionMaxWidth > 30) {
                int commentY = contentTop + lineHeight + 2;
                final List<FormattedCharSequence> lines = minecraft.font.split(Component.literal(description), descriptionMaxWidth);
                int drawn = 0;
                for (FormattedCharSequence line : lines) {
                    if (drawn >= descriptionLines) {
                        break;
                    }

                    graphics.drawString(minecraft.font, line, labelX, commentY, 0xFF606060, false);

                    commentY += lineHeight;
                    drawn++;
                }

            }

            // Thin top and bottom separators
            graphics.fill(left + 4, top, left + width - 4, top + 1, 0x08FFFFFF);
            graphics.fill(left + 4, top + height - 1, left + width - 4, top + height, 0x08FFFFFF);
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

    static class SearchBox extends EditBox {

        private float focusAnim = 0f;
        private final int accent;

        SearchBox(Font font, int x, int y, int width, int height, int accent) {
            super(font, x, y, width, height, Component.literal("Search"));
            this.accent = accent;
            setBordered(false);
            setTextColor(0xFFCCCCCC);
            setTextColorUneditable(0xFF555555);
            setMaxLength(128);
            setHint(Component.literal("Search..."));
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
            final boolean active = hovered || isFocused();

            focusAnim = active ? Math.min(1f, focusAnim + 0.12f) : Math.max(0f, focusAnim - 0.06f);

            // Background
            graphics.fill(x0, y0, x1, y1, 0xFF121212);

            // Magnifying glass icon
            final Minecraft minecraft = Minecraft.getInstance();
            final int[] accent = rgb(this.accent);
            final float time = focusAnim;
            final int iconRed = (int) (0x48 + ((accent[0] - 0x48) * time));
            final int iconGreen = (int) (0x48 + ((accent[1] - 0x48) * time));
            final int iconBlue = (int) (0x48 + ((accent[2] - 0x48) * time));
            final int iconColor = 0xFF000000 | (iconRed << 16) | (iconGreen << 8) | iconBlue;
            final float scale = 2.45f;
            final int iconOriginX = x0 + 3;
            final int iconOriginY = y0 + (height - (int)(minecraft.font.lineHeight * scale)) / 2 - 3;

            graphics.pose().pushPose();

            graphics.pose().translate(iconOriginX, iconOriginY, 0);
            graphics.pose().scale(scale, scale, 1f);
            graphics.drawString(minecraft.font, "\u2315", 0, 0, iconColor, false);

            graphics.pose().popPose();

            // Bottom line
            final int lineY = y1 - 1;
            final int dimLine = 0xFF2A2A2A;
            if (focusAnim > 0.01f) {
                final int lineRed = (int)(0x2A + ((accent[0] - 0x2A) * time));
                final int lineGreen = (int)(0x2A + ((accent[1] - 0x2A) * time));
                final int lineBlue = (int)(0x2A + ((accent[2] - 0x2A) * time));
                final int lineColor = 0xFF000000 | (lineRed << 16) | (lineGreen << 8) | lineBlue;

                graphics.fill(x0, lineY, x1, lineY + 1, lineColor);

                if (focusAnim > 0.1f) {
                    int glowAlpha = (int) (0x18 * focusAnim);
                    graphics.fill(x0, lineY - 1, x1, lineY, (glowAlpha << 24) | (accent[0] << 16) | (accent[1] << 8) | accent[2]);
                }

            }
            else {
                graphics.fill(x0, lineY, x1, lineY + 1, dimLine);
            }

            // Render text shifted past icon, vertically centered
            final int textPaddingX = 20;
            final int lineHeight = minecraft.font.lineHeight;
            final int offsetY = (height - lineHeight) / 2 + 1;

            graphics.pose().pushPose();

            graphics.pose().translate(textPaddingX, offsetY, 0);

            super.renderWidget(graphics, mx - textPaddingX, my - offsetY, partialTick);

            graphics.pose().popPose();
        }

    }

    static class ToggleButton extends AbstractWidget {

        private boolean toggled;
        private float animation;
        private final int accent;

        ToggleButton(int x, int y, int width, int height, boolean initial, int accent) {
            super(x, y, width, height, Component.empty());
            this.toggled = initial;
            this.animation = initial ? 1f : 0f;
            this.accent = accent;
        }

        boolean isToggled() {
            return toggled;
        }

        void setToggled(boolean toggled) {
            this.toggled = toggled;
            animation = toggled ? 1f : 0f;
        }

        @Override
        public void onClick(double mx, double my) {
            toggled = !toggled;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            animation = toggled ? Math.min(1f, animation + 0.2f) : Math.max(0f, animation - 0.2f);

            final int x0 = getX();
            final int y0 = getY();
            final int width = getWidth();
            final int height = getHeight();
            final int[] accent = rgb(this.accent);
            final int dark = 0xFF000000 | ((accent[0] / 4) << 16) | ((accent[1] / 4) << 8) | (accent[2] / 4);

            graphics.fill(x0, y0, x0 + width, y0 + height, lerp(0xFF2A2A2A, dark, animation));

            final int border = lerp(0xFF3A3A3A, this.accent, animation);
            graphics.fill(x0, y0, x0 + width, y0 + 1, border);
            graphics.fill(x0, y0 + height - 1, x0 + width, y0 + height, border);
            graphics.fill(x0, y0, x0 + 1, y0 + height, border);
            graphics.fill(x0 + width - 1, y0, x0 + width, y0 + height, border);

            final int padding = 3;
            final int knobWidth = 14;
            final int knobX = x0 + padding + (int) ((width - knobWidth - padding * 2) * animation);

            graphics.fill(knobX, y0 + padding, knobX + knobWidth, y0 + height - padding, lerp(0xFF555555, this.accent, animation));
        }

        private static int lerp(int from, int origin, float t) {
            final int finalAlpha = (from >> 24) & 0xFF;
            final int finalRed = (from >> 16) & 0xFF;
            final int finalGreen = (from >> 8) & 0xFF;
            final int finalBlue = from & 0xFF;
            final int totalAlpha = (origin >> 24) & 0xFF;
            final int totalRed = (origin >> 16) & 0xFF;
            final int totalGreen = (origin >> 8) & 0xFF;
            final int totalBlue = origin & 0xFF;
            return ((finalAlpha + (int) ((totalAlpha - finalAlpha) * t)) << 24) | ((finalRed + (int) ((totalRed - finalRed) * t)) << 16) | ((finalGreen + (int) ((totalGreen - finalGreen) * t)) << 8)  | (finalBlue + (int) ((totalBlue - finalBlue) * t));
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

        ConfigSlider(int x, int y, int w, int h, double min, double max, double value, boolean intMode, int accent) {
            super(x, y, w, h, Component.empty());
            this.min = min;
            this.max = max;
            this.value = Math.max(min, Math.min(max, value));
            this.intMode = intMode;
            this.accent = accent;
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
            double maxed = Math.max(0, Math.min(1, (mx - getX()) / (double) getWidth()));
            value = min + maxed * (max - min);
            if (intMode) {
                value = Math.round(value);
            }

        }

        @Override
        public void onClick(double mx, double my) {
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
        protected void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            final int x0 = getX();
            final int y0 = getY();
            final int width = getWidth();
            final int height = getHeight();
            final int[] accent = rgb(this.accent);

            graphics.fill(x0, y0, x0 + width, y0 + height, 0xFF1A1A1A);

            int fillWidth = (int) (width * frac());
            if (fillWidth > 0) {
                graphics.fill(x0, y0, x0 + fillWidth, y0 + height, 0xFF000000 | ((accent[0] / 3) << 16) | ((accent[1] / 3) << 8) | (accent[2] / 3));
            }

            boolean hovered = mx >= x0 && mx < x0 + width && my >= y0 && my < y0 + height;
            int border = (hovered || dragging) ? this.accent : 0xFF3A3A3A;

            graphics.fill(x0, y0, x0 + width, y0 + 1, border);
            graphics.fill(x0, y0 + height - 1, x0 + width, y0 + height, border);
            graphics.fill(x0, y0, x0 + 1, y0 + height, border);
            graphics.fill(x0 + width - 1, y0, x0 + width, y0 + height, border);

            int thumbX = x0 + Math.max(0, Math.min(width - 3, fillWidth - 1));
            graphics.fill(thumbX, y0, thumbX + 3, y0 + height, this.accent);

            final Minecraft minecraft = Minecraft.getInstance();
            final String text = intMode ? String.valueOf((long) getValueRaw()) : String.format("%.3f", getValueRaw());
            graphics.drawString(minecraft.font, text, x0 + (width - minecraft.font.width(text)) / 2, y0 + (height - minecraft.font.lineHeight) / 2 + 1, 0xFFD0D0D0, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            ;;
        }

    }

    static class EnumCycleButton extends AbstractWidget {

        private final Enum<?>[] values;
        private int idx;
        private float hoverAnim = 0f;
        private final int accent;

        EnumCycleButton(int x, int y, int width, int height, Enum<?>[] values, Enum<?> current, int accent) {
            super(x, y, width, height, Component.empty());
            this.values = values;
            this.accent = accent;
            this.idx = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i] == current) {
                    idx = i;
                    break;
                }

            }

        }

        Enum<?> getCurrent() {
            return values[idx];
        }

        void setCurrent(Enum<?> value) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] == value) {
                    idx = i;
                    return;
                }

            }

        }

        @Override
        public void onClick(double mx, double my) {
            idx = (idx + 1) % values.length;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            final int x0 = getX();
            final int y0 = getY();
            final int width = getWidth();
            final int height = getHeight();
            final boolean hovering = mx >= x0 && mx < x0 + width && my >= y0 && my < y0 + height;
            hoverAnim = hovering ? Math.min(1f, hoverAnim + 0.15f) : Math.max(0f, hoverAnim - 0.1f);

            graphics.fill(x0, y0, x0 + width, y0 + height, 0xFF181818);
            final int border = isHovered ? accent : 0xFF3A3A3A;
            graphics.fill(x0, y0, x0 + width, y0 + 1, border);
            graphics.fill(x0, y0 + height - 1, x0 + width, y0 + height, border);
            graphics.fill(x0, y0, x0 + 1, y0 + height, border);
            graphics.fill(x0 + width - 1, y0, x0 + width, y0 + height, border);

            final Minecraft minecraft = Minecraft.getInstance();
            final String text = prettify(values[idx].name());

            graphics.drawString(minecraft.font, text, x0 + (width - minecraft.font.width(text)) / 2, y0 + (height - minecraft.font.lineHeight) / 2 + 1, 0xFFD0D0D0, false);

            final int alphaY = y0 + (height - minecraft.font.lineHeight) / 2 + 1;
            graphics.drawString(minecraft.font, "\u2190", x0 + 3, alphaY, border, false);
            graphics.drawString(minecraft.font, "\u2192", x0 + width - 10, alphaY, border, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            ;;
        }

    }

    static class StyledEditBox extends EditBox {

        private float focusAnim = 0f;
        private final int accent;

        StyledEditBox(Font font, int x, int y, int width, int height, Component message, int accent) {
            super(font, x, y, width, height, message);
            this.accent = accent;
            setBordered(false);
            setTextColor(0xFFD0D0D0);
            setTextColorUneditable(0xFF666666);
            setMaxLength(512);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            final int x0 = getX();
            final int y0 = getY();
            final int x1 = x0 + getWidth();
            final int y1 = y0 + getHeight();
            boolean enable = (mx >= x0 && mx < x1 && my >= y0 && my < y1) || isFocused();
            focusAnim = enable ? Math.min(1f, focusAnim + 0.15f) : Math.max(0f, focusAnim - 0.1f);

            graphics.fill(x0, y0, x1, y1, 0xFF161616);

            int border = focusAnim > 0 ? accent : 0xFF3A3A3A;
            graphics.fill(x0, y0, x1, y0 + 1, border);
            graphics.fill(x0, y1 - 1, x1, y1, border);
            graphics.fill(x0, y0, x0 + 1, y1, border);
            graphics.fill(x1 - 1, y0, x1, y1, border);

            int lineHeight = Minecraft.getInstance().font.lineHeight;
            int offsetY = (this.height - lineHeight) / 2 + 1;
            int paddingX = 5;

            graphics.enableScissor(x0 + 1, y0 + 1, x1 - 1, y1 - 1);

            graphics.pose().pushPose();
            graphics.pose().translate(paddingX, offsetY, 0);
            super.renderWidget(graphics, mx - paddingX, my - offsetY, partialTick);
            graphics.pose().popPose();

            graphics.disableScissor();
        }

    }

    static class FlatButton extends Button {

        private float hoverAnim = 0f;
        private final int accent;

        FlatButton(int x, int y, int width, int height, Component message, OnPress onPress, int accent) {
            super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
            this.accent = accent;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mx, int my, float partialTick) {
            final int x0 = getX();
            final int y0 = getY();
            final int x1 = x0 + getWidth();
            final int y1 = y0 + getHeight();
            boolean hovering = mx >= x0 && mx < x1 && my >= y0 && my < y1;
            hoverAnim = hovering ? Math.min(1f, hoverAnim + 0.15f) : Math.max(0f, hoverAnim - 0.1f);

            graphics.fill(x0, y0, x1, y1, 0xFF141414);
            graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, hovering ? 0xFF1C1C1C : 0xFF181818);

            int borderWidth = 2 + Math.round(hoverAnim);
            graphics.fill(x0, y0, x0 + borderWidth, y1, hoverAnim > 0 ? accent : 0x40FFFFFF);

            if (hoverAnim > 0f) {
                final int[] accent = rgb(this.accent);
                final int defWidth = Math.min(60, getWidth() / 3);
                final int maxAlpha = (int) (0x20 * hoverAnim);
                for (int x = 0; x < defWidth; x++) {
                    int alpha = (int) (maxAlpha * (1f - (float) x / defWidth));
                    if (alpha > 0) {
                        graphics.fill(x0 + x, y0, x0 + x + 1, y1, (alpha << 24) | (accent[0] << 16) | (accent[1] << 8) | accent[2]);
                    }

                }

            }

            final int color = hovering ? 0xFF2A2A2A : 0xFF222222;
            graphics.fill(x0, y0, x1, y0 + 1, color);
            graphics.fill(x0, y1 - 1, x1, y1, color);
            graphics.fill(x0, y0, x0 + 1, y1, color);
            graphics.fill(x1 - 1, y0, x1, y1, color);

            final Minecraft minecraft = Minecraft.getInstance();
            graphics.drawCenteredString(minecraft.font, getMessage(), x0 + width / 2, y0 + (this.height - minecraft.font.lineHeight) / 2 + 1, active ? 0xFFD0D0D0 : 0xFF666666);
        }

    }

}
