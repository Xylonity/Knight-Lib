package dev.xylonity.knightlib.client.screen.config;

import dev.xylonity.knightlib.api.config.ConfigEntry;
import dev.xylonity.knightlib.api.config.ConfigRange;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.RecordComponent;
import java.util.*;

import static dev.xylonity.knightlib.client.screen.config.KnightLibConfigScreen.*;

abstract class ConfigPopup {

    protected static final int PAD = 3;
    private static final int GAP = 3;
    private static final int OPEN_MS = 220;
    private static final int CLOSE_MS = 130;

    protected final SelectionButton owner;
    protected final int accent;
    protected final Font font = Minecraft.getInstance().font;
    protected final long openedAt = Util.getMillis();
    private long closedAt = 0L;

    private final int anchorX;
    private final int anchorY;
    private final int anchorWidth;
    private final int anchorHeight;
    private int screenWidth;
    private int screenHeight;
    private boolean above;

    protected int x;
    protected int width;
    private float height;

    ConfigPopup(SelectionButton owner) {
        this.owner = owner;
        this.accent = owner.accent;
        this.anchorX = owner.getX();
        this.anchorY = owner.getY();
        this.anchorWidth = owner.getWidth();
        this.anchorHeight = owner.getHeight();
    }

    Object owner() {
        return owner;
    }

    void open(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        this.width = Mth.clamp(Math.max(desiredWidth(), anchorWidth), 60, screenWidth - 16);
        this.x = Mth.clamp(anchorX + anchorWidth - width, 8, screenWidth - 8 - width);

        final int wanted = desiredHeight();
        final int below = screenHeight - (anchorY + anchorHeight + GAP) - 8;
        final int over = anchorY - GAP - 8;
        this.above = below < wanted && over > below;
        this.height = Math.min(wanted, maxHeight());
    }

    private int maxHeight() {
        return Math.max(30, above ? anchorY - GAP - 8 : screenHeight - (anchorY + anchorHeight + GAP) - 8);
    }

    protected int top() {
        return above ? anchorY - GAP - Math.round(height) : anchorY + anchorHeight + GAP;
    }

    protected int bottom() {
        return top() + Math.round(height);
    }

    boolean isClosing() {
        return closedAt != 0L;
    }

    boolean isGone() {
        return isClosing() && Util.getMillis() - closedAt > CLOSE_MS;
    }

    void close() {
        if (!isClosing()) {
            closedAt = Util.getMillis();
            onClosed();
        }

    }

    protected boolean isInside(double mx, double my) {
        return mx >= x && mx < x + width && my >= top() && my < bottom();
    }

    private float progress() {
        final long now = Util.getMillis();
        return isClosing() ? 1f - ease((now - closedAt) / (float) CLOSE_MS) : ease((now - openedAt) / (float) OPEN_MS);
    }

    final void render(GuiGraphics graphics, Font font, int mx, int my) {
        // The size follows the content, adding or removing entries resizes it
        height = approach(height, Math.min(desiredHeight(), maxHeight()), 18f);

        final float progress = progress();
        graphics.fill(0, 0, screenWidth, screenHeight, withAlpha(0x000000, (int) (0x48 * progress)));

        final int top = top();
        final int bottom = bottom();

        // Unfolds from the button's side
        final int shown = Math.max(6, Math.round((bottom - top) * (0.4f + 0.6f * progress)));
        final int frameTop = above ? bottom - shown : top;
        final int frameBottom = above ? bottom : top + shown;

        graphics.pose().pushPose();
        graphics.pose().translate(0f, 0f, 200f);
        beginFade(graphics, progress);

        frame(graphics, x, frameTop, x + width, frameBottom, opaque(SURFACE), opaque(FIELD_BORDER), 2);
        graphics.enableScissor(x + 1, frameTop + 1, x + width - 1, frameBottom - 1);
        renderContent(graphics, isClosing() ? -10000 : mx, isClosing() ? -10000 : my, top, bottom);
        graphics.disableScissor();

        if (!isClosing()) {
            renderOverlay(graphics, mx, my, screenWidth, screenHeight);
        }

        endFade(graphics, progress);
        graphics.pose().popPose();
    }

    abstract int desiredWidth();

    abstract int desiredHeight();

    abstract void renderContent(GuiGraphics graphics, int mx, int my, int top, int bottom);

    void renderOverlay(GuiGraphics graphics, int mx, int my, int screenWidth, int screenHeight) {
        ;;
    }

    void onClosed() {
        ;;
    }

    boolean mouseClicked(double mx, double my, int button) {
        return isInside(mx, my);
    }

    void mouseReleased(double mx, double my, int button) {
        ;;
    }

    void mouseDragged(double mx, double my, int button, double dx, double dy) {
        ;;
    }

    void mouseScrolled(double mx, double my, double scrollY) {
        ;;
    }

    void tick() {
        ;;
    }

    void keyPressed(int key, int scancode, int modifiers) {
        ;;
    }

    void charTyped(char c, int modifiers) {
        ;;
    }

    static final class Scroller {

        private static final int BAR_ZONE = 5;

        double scroll = 0;
        double target = 0;

        private int barLeft;
        private int barTop;
        private int barBottom;
        private int content;

        private boolean dragging = false;
        private double dragOffset = 0;
        private float hoverAnim = 0f;

        void update(int content, int viewport) {
            final double max = Math.max(0, content - viewport);
            target = Mth.clamp(target, 0, max);
            scroll = dragging || Math.abs(target - scroll) < 0.3 ? target : scroll + (target - scroll) * (1.0 - Math.exp(-20.0 * dt));
            scroll = Mth.clamp(scroll, 0, max);
        }

        private int viewport() {
            return barBottom - barTop;
        }

        private boolean scrollable() {
            return content > viewport() && viewport() > 0;
        }

        private int thumbHeight() {
            return Math.max(10, viewport() * viewport() / Math.max(1, content));
        }

        private int thumbY() {
            final double max = Math.max(1, content - viewport());
            return barTop + (int) ((viewport() - thumbHeight()) * (scroll / max));
        }

        boolean isOverBar(double mx, double my) {
            return scrollable() && mx >= barLeft && mx < barLeft + BAR_ZONE && my >= barTop && my < barBottom;
        }

        boolean mouseClicked(double mx, double my) {
            if (!isOverBar(mx, my)) {
                return false;
            }

            final int thumbY = thumbY();
            final int thumb = thumbHeight();
            if (my < thumbY || my >= thumbY + thumb) {
                final double span = Math.max(1, viewport() - thumb);
                target = Mth.clamp((my - barTop - thumb / 2.0) / span, 0.0, 1.0) * (content - viewport());
            }

            dragging = true;
            dragOffset = my - thumbY();
            return true;
        }

        boolean mouseDragged(double my) {
            if (!dragging) {
                return false;
            }

            final double span = Math.max(1, viewport() - thumbHeight());
            jumpTo(Mth.clamp((my - dragOffset - barTop) / span, 0.0, 1.0) * (content - viewport()));
            return true;
        }

        void mouseReleased() {
            dragging = false;
        }

        void scrollBy(double amount) {
            target += amount;
        }

        void jumpTo(double position) {
            target = position;
            scroll = position;
        }

        void reveal(int itemTop, int itemBottom, int viewport) {
            if (itemTop < target) {
                target = itemTop;
            }
            else if (itemBottom > target + viewport) {
                target = itemBottom - viewport;
            }

        }

        void renderEdges(GuiGraphics graphics, int left, int right, int top, int bottom, int content, int mx, int my) {
            this.barLeft = right;
            this.barTop = top;
            this.barBottom = bottom;
            this.content = content;

            if (!scrollable()) {
                dragging = false;
                return;
            }

            final double max = content - viewport();
            final float topFade = (float) Mth.clamp(scroll / 10.0, 0.0, 1.0);
            final float bottomFade = (float) Mth.clamp((max - scroll) / 10.0, 0.0, 1.0);
            if (topFade > 0.01f) {
                graphics.fillGradient(left, top, right, top + 8, withAlpha(SURFACE, (int) (0xF0 * topFade)), withAlpha(SURFACE, 0));
            }
            if (bottomFade > 0.01f) {
                graphics.fillGradient(left, bottom - 8, right, bottom, withAlpha(SURFACE, 0), withAlpha(SURFACE, (int) (0xF0 * bottomFade)));
            }

            // Widens and lights up under the cursor or while dragged
            final boolean engaged = dragging || isOverBar(mx, my);
            hoverAnim = approach(hoverAnim, engaged ? 1f : 0f, engaged ? 18f : 8f);

            final int thumbY = thumbY();
            final int thumbWidth = 2 + Math.round(hoverAnim);
            final int thumbX = right + 1 + (3 - thumbWidth) / 2;
            graphics.fill(thumbX, thumbY, thumbX + thumbWidth, thumbY + thumbHeight(), opaque(mixRgb(0x4A4A52, 0x8A8A92, dragging ? 1f : hoverAnim)));
        }

    }

    static final class Select extends ConfigPopup {

        private static final int ROW = 14;
        private static final int SEARCH = 16;
        private static final int FOOTER = 14;
        private static final int VISIBLE_ROWS = 9;
        private static final int SEARCH_THRESHOLD = 8;
        private static final int TOOLTIP_DELAY_MS = 300;

        private final List<Object> entries;
        private final List<Object> filtered = new ArrayList<>();
        private final SearchBox search;
        private final Scroller scroller = new Scroller();
        private final Map<Object, Float> checkAnims = new HashMap<>();
        private final Map<Object, Float> hoverAnims = new HashMap<>();
        private final int widest;

        private int keyboardIndex = -1;
        private boolean revealPending;
        private float clearHoverAnim = 0f;

        private Object cutEntry = null;
        private long cutSince = 0L;

        private int listTop;
        private int listBottom;
        private int listRight;
        private int footerY;

        Select(SelectionButton owner) {
            super(owner);

            final LinkedHashSet<Object> ordered = new LinkedHashSet<>();
            if (owner.multiple) {
                ordered.addAll(owner.values());
                ordered.addAll(owner.choices);
            }
            else {
                for (Object value : owner.values()) {
                    if (!owner.choices.contains(value) && !"".equals(value)) {
                        ordered.add(value);
                    }

                }

                ordered.addAll(owner.choices);
            }

            this.entries = new ArrayList<>(ordered);

            int widestLabel = 0;
            for (Object entry : entries) {
                widestLabel = Math.max(widestLabel, font.width(owner.label(entry)));
            }
            this.widest = widestLabel;

            for (Object value : owner.values()) {
                checkAnims.put(value, 1f);
            }

            if (entries.size() > SEARCH_THRESHOLD) {
                this.search = new SearchBox(font, 0, 0, 100, SEARCH, accent);
                this.search.setFocused(true);
                this.search.setResponder(query -> refilter());
            }
            else {
                this.search = null;
            }

            refilter();

            if (!owner.multiple && !owner.values().isEmpty()) {
                keyboardIndex = filtered.indexOf(owner.values().get(0));
                revealPending = keyboardIndex >= 0;
            }

        }

        private void refilter() {
            final String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
            filtered.clear();
            for (Object entry : entries) {
                if (query.isEmpty() || owner.label(entry).toLowerCase(Locale.ROOT).contains(query) || String.valueOf(entry).toLowerCase(Locale.ROOT).contains(query)) {
                    filtered.add(entry);
                }

            }

            final String typed = typedTag(query);
            if (typed != null) {
                filtered.add(0, typed);
            }

            keyboardIndex = query.isEmpty() || filtered.isEmpty() ? -1 : 0;
            scroller.jumpTo(0);
        }

        private String typedTag(String query) {
            if (!owner.registryIds || !owner.meta.tags() || Minecraft.getInstance().level != null || !query.startsWith("#")) {
                return null;
            }

            final ResourceLocation tag = ResourceLocation.tryParse(query.substring(1));
            if (tag == null || tag.getPath().isEmpty()) {
                return null;
            }

            final String value = "#" + tag;
            return entries.contains(value) ? null : value;
        }

        private boolean atLimit() {
            return owner.multiple && owner.maxSize() > 0 && owner.values().size() >= owner.maxSize();
        }

        @Override
        int desiredWidth() {
            return Mth.clamp(widest + (owner.multiple ? 34 : 30), 130, 240);
        }

        @Override
        int desiredHeight() {
            final int rows = Mth.clamp(entries.size(), 1, VISIBLE_ROWS);
            return PAD * 2 + (search != null ? SEARCH + 3 : 0) + rows * ROW + (owner.multiple ? FOOTER + 2 : 0);
        }

        @Override
        void renderContent(GuiGraphics graphics, int mx, int my, int top, int bottom) {
            final long now = Util.getMillis();

            if (search != null) {
                search.setX(x + PAD);
                search.setY(top + PAD);
                search.setWidth(width - PAD * 2);
                search.render(graphics, mx, my, 0f);
            }

            listTop = top + PAD + (search != null ? SEARCH + 3 : 0);
            listBottom = bottom - PAD - (owner.multiple ? FOOTER + 2 : 0);
            footerY = listBottom + 2;

            final int viewport = Math.max(0, listBottom - listTop);
            final int content = filtered.size() * ROW;
            if (revealPending && viewport > 0) {
                scroller.jumpTo(Math.max(0, keyboardIndex * ROW - (viewport - ROW) / 2));
                revealPending = false;
            }
            scroller.update(content, viewport);

            final int rowLeft = x + PAD;
            final int rowRight = x + width - PAD - (content > viewport ? 4 : 0);
            listRight = rowRight;

            KnightLibConfigScreen.enableScissor(graphics, x + 1, listTop, x + width - 1, listBottom);

            if (filtered.isEmpty()) {
                final String text = I18n.get("knightlib.config.no_matches");
                graphics.drawString(font, text, x + (width - font.width(text)) / 2, listTop + (viewport - font.lineHeight) / 2 + 1, opaque(TEXT_MUTED), false);
            }

            final boolean limited = atLimit();
            final int first = Math.max(0, (int) (scroller.scroll / ROW));
            final int last = Math.min(filtered.size(), first + viewport / ROW + 2);
            Object cutUnderMouse = null;

            for (int i = first; i < last; i++) {
                final Object entry = filtered.get(i);
                final int rowY = listTop + i * ROW - (int) scroller.scroll;

                final boolean selected = owner.values().contains(entry);
                final boolean blocked = limited && !selected;
                final boolean underMouse = mx >= rowLeft && mx < rowRight && my >= Math.max(rowY, listTop) && my < Math.min(rowY + ROW, listBottom);
                final boolean hovered = !blocked && underMouse;

                final float hover = approach(hoverAnims.getOrDefault(entry, 0f), hovered || i == keyboardIndex ? 1f : 0f, hovered ? 20f : 10f);
                hoverAnims.put(entry, hover);

                final float check = approach(checkAnims.getOrDefault(entry, 0f), selected ? 1f : 0f, 22f);
                checkAnims.put(entry, check);

                final float appear = ease((now - openedAt - Math.min(i - first, 12) * 14L) / 200f);
                final int slide = Math.round((1f - appear) * 4f);

                if (hover > 0.01f) {
                    rounded(graphics, rowLeft, rowY, rowRight, rowY + ROW, withAlpha(0xFFFFFF, (int) (0x0C * hover)), 1);
                }

                int labelX = rowLeft + 5 + slide;
                if (owner.multiple) {
                    final int boxX = rowLeft + 3 + slide;
                    final int boxY = rowY + (ROW - 9) / 2;
                    frame(graphics, boxX, boxY, boxX + 9, boxY + 9, opaque(mixRgb(INPUT, accent, 0.25f * check)), opaque(mixRgb(blocked ? FIELD_BORDER : FIELD_BORDER_HOVER, accent, check)), 1);

                    if (check > 0.02f) {
                        final int inset = Math.round((1f - check) * 3.5f);
                        graphics.fill(boxX + 1 + inset, boxY + 1 + inset, boxX + 8 - inset, boxY + 8 - inset, opaque(accent));
                    }
                    if (check > 0.6f) {
                        ICON_CHECK.draw(graphics, boxX + 2, boxY + 3, withAlpha(onAccent(accent), (int) (0xFF * (check - 0.6f) / 0.4f)));
                    }

                    labelX = boxX + 9 + 5;
                }

                final int textAlpha = (int) (0xFF * appear);
                if (textAlpha < 0x08) {
                    continue;
                }

                final int labelColor = blocked ? TEXT_FAINT : mixRgb(mixRgb(TEXT_SOFT, TEXT, hover), 0xFFFFFF, selected ? 1f : 0f);
                final int textY = rowY + (ROW - font.lineHeight) / 2 + 1;

                int labelRight = rowRight - 4;
                if (!owner.multiple && selected) {
                    ICON_CHECK.draw(graphics, rowRight - 5 - ICON_CHECK.width, rowY + (ROW - ICON_CHECK.height) / 2, withAlpha(accent, textAlpha));
                    labelRight -= ICON_CHECK.width + 4;
                }

                if (owner.registryIds && entry instanceof String text) {
                    final String id = text.startsWith("#") ? text.substring(1) : text;
                    if (!id.startsWith("minecraft:") && id.indexOf(':') > 0) {
                        final String namespace = id.substring(0, id.indexOf(':'));
                        final int namespaceX = labelRight - font.width(namespace);
                        graphics.drawString(font, namespace, namespaceX, textY, withAlpha(TEXT_FAINT, textAlpha), false);
                        labelRight = namespaceX - 6;
                    }

                }

                final String label = owner.label(entry);
                if (underMouse && font.width(label) > labelRight - labelX) {
                    cutUnderMouse = entry;
                }

                graphics.drawString(font, ellipsize(font, label, labelRight - labelX), labelX, textY, withAlpha(labelColor, textAlpha), false);
            }

            if (cutUnderMouse != cutEntry) {
                cutEntry = cutUnderMouse;
                cutSince = now;
            }

            scroller.renderEdges(graphics, x + 1, rowRight, listTop, listBottom, content, mx, my);
            graphics.disableScissor();

            if (owner.multiple) {
                renderFooter(graphics, mx, my);
            }

        }

        @Override
        void renderOverlay(GuiGraphics graphics, int mx, int my, int screenWidth, int screenHeight) {
            if (cutEntry != null) {
                final float fade = ease((Util.getMillis() - cutSince - TOOLTIP_DELAY_MS) / 160f);
                drawTooltip(graphics, font, List.of(TooltipLine.of(Component.literal(owner.label(cutEntry)))), mx, my, fade, screenWidth, screenHeight);
            }

        }

        private void renderFooter(GuiGraphics graphics, int mx, int my) {
            final int count = owner.values().size();
            final String text = owner.maxSize() > 0 ? I18n.get("knightlib.config.selected_max", count, owner.maxSize()) : I18n.get("knightlib.config.selected", count);
            final int textY = footerY + (FOOTER - font.lineHeight) / 2 + 1;
            graphics.drawString(font, text, x + PAD + 5, textY, opaque(atLimit() ? TEXT_SOFT : TEXT_MUTED), false);

            final String clear = I18n.get("knightlib.config.clear");
            final int clearX = x + width - PAD - 5 - font.width(clear);
            final boolean canClear = count > 0 && owner.minSize() == 0;
            final boolean clearHovered = canClear && mx >= clearX - 3 && mx < x + width - PAD && my >= footerY && my < footerY + FOOTER;
            clearHoverAnim = approach(clearHoverAnim, clearHovered ? 1f : 0f, 18f);

            final int clearColor = !canClear ? TEXT_FAINT : mixRgb(TEXT_DIM, 0xFFFFFF, clearHoverAnim);
            graphics.drawString(font, clear, clearX, textY, opaque(clearColor), false);
        }

        private int rowAt(double mx, double my) {
            if (mx < x + PAD || mx >= listRight || my < listTop || my >= listBottom) {
                return -1;
            }

            final int index = (int) ((my - listTop + scroller.scroll) / ROW);
            return index >= 0 && index < filtered.size() ? index : -1;
        }

        private void choose(Object entry) {
            if (!entries.contains(entry)) {
                entries.add(0, entry);
            }

            if (!owner.multiple) {
                owner.setValues(List.of(entry));
                playClickSound();
                close();
                return;
            }

            final List<Object> values = new ArrayList<>(owner.values());
            if (values.contains(entry)) {
                if (values.size() <= owner.minSize()) {
                    return;
                }

                values.remove(entry);
            }
            else if (!atLimit()) {
                values.add(entry);
            }
            else {
                return;
            }

            owner.setValues(values);
            playClickSound();
        }

        @Override
        boolean mouseClicked(double mx, double my, int button) {
            if (!isInside(mx, my)) {
                return false;
            }

            if (scroller.mouseClicked(mx, my)) {
                return true;
            }

            if (search != null && search.isMouseOver(mx, my)) {
                search.setFocused(true);
                search.mouseClicked(mx, my, button);
                return true;
            }

            if (owner.multiple && my >= footerY && mx >= x + width / 2 && !owner.values().isEmpty() && owner.minSize() == 0) {
                owner.setValues(List.of());
                playClickSound();
                return true;
            }

            final int index = rowAt(mx, my);
            if (index >= 0 && button == 0) {
                keyboardIndex = index;
                choose(filtered.get(index));
            }

            return true;
        }

        @Override
        void mouseScrolled(double mx, double my, double scrollY) {
            scroller.scrollBy(-scrollY * ROW * 2);
        }

        @Override
        void tick() {
            if (search != null) {
                search.tick();
            }

        }

        @Override
        void mouseDragged(double mx, double my, int button, double dx, double dy) {
            scroller.mouseDragged(my);
        }

        @Override
        void mouseReleased(double mx, double my, int button) {
            scroller.mouseReleased();
        }

        @Override
        void keyPressed(int key, int scancode, int modifiers) {
            if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_UP) {
                if (!filtered.isEmpty()) {
                    final int step = key == GLFW.GLFW_KEY_DOWN ? 1 : -1;
                    keyboardIndex = keyboardIndex < 0 ? (step > 0 ? 0 : filtered.size() - 1) : Mth.clamp(keyboardIndex + step, 0, filtered.size() - 1);
                    scroller.reveal(keyboardIndex * ROW, keyboardIndex * ROW + ROW, listBottom - listTop);
                }

                return;
            }

            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (keyboardIndex >= 0 && keyboardIndex < filtered.size()) {
                    choose(filtered.get(keyboardIndex));
                }

                return;
            }

            if (search != null) {
                search.keyPressed(key, scancode, modifiers);
            }

        }

        @Override
        void charTyped(char c, int modifiers) {
            if (search != null) {
                search.charTyped(c, modifiers);
            }

        }

    }

    static final class ListEditor extends ConfigPopup {

        private static final int ROW = 20;
        private static final int ADD = 16;
        private static final int REMOVE = 14;
        private static final int VISIBLE_ROWS = 7;

        private final List<StyledEditBox> boxes = new ArrayList<>();
        private final List<Long> addedAt = new ArrayList<>();
        private final Map<StyledEditBox, Float> removeHover = new IdentityHashMap<>();
        private final Scroller scroller = new Scroller();
        private StyledEditBox focusedBox;
        private float addHoverAnim = 0f;

        private int listTop;
        private int listBottom;

        ListEditor(SelectionButton owner) {
            super(owner);

            final List<Object> values = owner.values();
            for (int i = 0; i < values.size(); i++) {
                addBox(editText(values.get(i)), openedAt + Math.min(i, 8) * 18L);
            }

            while (boxes.size() < owner.minSize()) {
                addBox("", openedAt + Math.min(boxes.size(), 8) * 18L);
            }

            if (boxes.isEmpty()) {
                focus(addBox("", openedAt));
            }

        }

        private static String editText(Object value) {
            if (value instanceof Double || value instanceof Float) {
                return displayLabel(value, false);
            }

            return String.valueOf(value);
        }

        private StyledEditBox addBox(String text, long appearAt) {
            final StyledEditBox box = new StyledEditBox(font, 0, 0, 100, 16, Component.empty(), accent, false);
            box.setMaxLength(256);
            box.setValue(text);
            box.setValidator(raw -> raw.isBlank() || parse(raw) != null);
            box.setResponder(changed -> push());

            boxes.add(box);
            addedAt.add(appearAt);
            return box;
        }

        private boolean atLimit() {
            return owner.maxSize() > 0 && boxes.size() >= owner.maxSize();
        }

        private Object parse(String raw) {
            final String text = raw.trim();
            if (text.isEmpty()) {
                return null;
            }

            final ConfigEntry meta = owner.meta;
            final Class<?> type = owner.elementType;
            try {
                if (type == String.class) {
                    return text;
                }

                final double number;
                final Object value;
                if (type == Integer.class) {
                    value = Integer.parseInt(text);
                    number = (Integer) value;
                }
                else if (type == Long.class) {
                    value = Long.parseLong(text);
                    number = (Long) value;
                }
                else if (type == Float.class) {
                    value = Float.parseFloat(text);
                    number = (Float) value;
                }
                else if (type == Double.class) {
                    value = Double.parseDouble(text);
                    number = (Double) value;
                }
                else {
                    return text;
                }

                return number < meta.min() || number > meta.max() || Double.isNaN(number) ? null : value;
            }
            catch (NumberFormatException exception) {
                return null;
            }

        }

        private void push() {
            final List<Object> values = new ArrayList<>();
            for (StyledEditBox box : boxes) {
                final Object value = parse(box.getValue());
                if (value != null) {
                    values.add(value);
                }

            }

            owner.setValues(values);
        }

        private void focus(StyledEditBox box) {
            for (StyledEditBox other : boxes) {
                other.setFocused(other == box);
            }

            focusedBox = box;
            final int index = boxes.indexOf(box);
            if (index >= 0) {
                scroller.reveal(index * ROW, index * ROW + ROW, Math.max(ROW, listBottom - listTop));
            }

        }

        private void removeAt(int index) {
            final StyledEditBox removed = boxes.remove(index);
            addedAt.remove(index);
            removeHover.remove(removed);

            if (removed == focusedBox) {
                focusedBox = null;
                if (!boxes.isEmpty()) {
                    focus(boxes.get(Math.max(0, index - 1)));
                }

            }

            push();
        }

        private void addAndFocus() {
            if (atLimit()) {
                return;
            }

            focus(addBox("", Util.getMillis()));
            push();
        }

        @Override
        int desiredWidth() {
            return 200;
        }

        @Override
        int desiredHeight() {
            return PAD * 2 + Math.min(boxes.size(), VISIBLE_ROWS) * ROW + ADD + 1;
        }

        @Override
        void renderContent(GuiGraphics graphics, int mx, int my, int top, int bottom) {
            final long now = Util.getMillis();

            listTop = top + PAD;
            listBottom = bottom - PAD - ADD - 1;
            final int viewport = Math.max(0, listBottom - listTop);
            final int content = boxes.size() * ROW;
            scroller.update(content, viewport);

            final int rowLeft = x + PAD;
            final int rowRight = x + width - PAD - (content > viewport ? 4 : 0);

            KnightLibConfigScreen.enableScissor(graphics, x + 1, listTop, x + width - 1, listBottom);

            final boolean removable = boxes.size() > owner.minSize();
            for (int i = 0; i < boxes.size(); i++) {
                final StyledEditBox box = boxes.get(i);
                final int rowY = listTop + i * ROW - (int) scroller.scroll;

                box.setX(rowLeft);
                box.setY(rowY + 2);
                box.setWidth(rowRight - REMOVE - 4 - rowLeft);

                if (rowY + ROW < listTop || rowY > listBottom) {
                    continue;
                }

                // New values slide in from the left
                final float appear = ease((now - addedAt.get(i)) / 200f);
                graphics.pose().pushPose();
                graphics.pose().translate((1f - appear) * -6f, 0f, 0f);

                box.render(graphics, mx, my, 0f);

                final int removeX = rowRight - REMOVE;
                final int removeY = rowY + (ROW - REMOVE) / 2;
                final boolean removeHovered = removable && mx >= removeX && mx < removeX + REMOVE && my >= removeY && my < removeY + REMOVE && my >= listTop && my < listBottom;
                final float hover = approach(removeHover.getOrDefault(box, 0f), removeHovered ? 1f : 0f, 18f);
                removeHover.put(box, hover);

                frame(graphics, removeX, removeY, removeX + REMOVE, removeY + REMOVE, opaque(mixRgb(FIELD, mixRgb(FIELD, ERROR, 0.35f), hover)), opaque(mixRgb(FIELD_BORDER, ERROR, hover * 0.8f)), 1);
                ICON_CLEAR.draw(graphics, removeX + (REMOVE - ICON_CLEAR.width) / 2, removeY + (REMOVE - ICON_CLEAR.height) / 2, opaque(removable ? mixRgb(0x8A8A92, 0xFFFFFF, hover) : TEXT_FAINT));

                graphics.pose().popPose();
            }

            scroller.renderEdges(graphics, x + 1, rowRight, listTop, listBottom, content, mx, my);
            graphics.disableScissor();

            final int addTop = listBottom + 1;
            final boolean limited = atLimit();
            final boolean addHovered = !limited && mx >= rowLeft && mx < x + width - PAD && my >= addTop && my < addTop + ADD;
            addHoverAnim = approach(addHoverAnim, addHovered ? 1f : 0f, 18f);
            if (addHoverAnim > 0.01f) {
                rounded(graphics, rowLeft, addTop, x + width - PAD, addTop + ADD, withAlpha(0xFFFFFF, (int) (0x0C * addHoverAnim)), 1);
            }

            final int addColor = limited ? TEXT_FAINT : mixRgb(TEXT_DIM, 0xFFFFFF, addHoverAnim);
            final String add = I18n.get("knightlib.config.add");
            final int textY = addTop + (ADD - font.lineHeight) / 2 + 1;
            ICON_PLUS.draw(graphics, rowLeft + 5, addTop + (ADD - ICON_PLUS.height) / 2, opaque(limited ? TEXT_FAINT : mixRgb(accent, 0xFFFFFF, addHoverAnim * 0.4f)));
            graphics.drawString(font, add, rowLeft + 5 + ICON_PLUS.width + 5, textY, opaque(addColor), false);

            if (owner.maxSize() > 0) {
                final String count = boxes.size() + " / " + owner.maxSize();
                graphics.drawString(font, count, x + width - PAD - 5 - font.width(count), textY, opaque(limited ? TEXT_SOFT : TEXT_MUTED), false);
            }

        }

        @Override
        boolean mouseClicked(double mx, double my, int button) {
            if (!isInside(mx, my)) {
                return false;
            }

            if (scroller.mouseClicked(mx, my)) {
                return true;
            }

            if (my >= listTop && my < listBottom) {
                for (int i = 0; i < boxes.size(); i++) {
                    final StyledEditBox box = boxes.get(i);
                    final int removeX = box.getX() + box.getWidth() + 4;
                    final int removeY = box.getY() - 2 + (ROW - REMOVE) / 2;
                    if (mx >= removeX && mx < removeX + REMOVE && my >= removeY && my < removeY + REMOVE) {
                        if (boxes.size() > owner.minSize()) {
                            removeAt(i);
                            playClickSound();
                        }

                        return true;
                    }

                    if (box.isMouseOver(mx, my)) {
                        focus(box);
                        box.mouseClicked(mx, my, button);
                        return true;
                    }

                }

            }
            else if (my >= listBottom + 1 && !atLimit()) {
                playClickSound();
                addAndFocus();
            }

            return true;
        }

        @Override
        void mouseDragged(double mx, double my, int button, double dx, double dy) {
            if (!scroller.mouseDragged(my) && focusedBox != null) {
                focusedBox.mouseDragged(mx, my, button, dx, dy);
            }

        }

        @Override
        void mouseReleased(double mx, double my, int button) {
            scroller.mouseReleased();
        }

        @Override
        void mouseScrolled(double mx, double my, double scrollY) {
            scroller.scrollBy(-scrollY * ROW);
        }

        @Override
        void tick() {
            for (StyledEditBox box : boxes) {
                box.tick();
            }

        }

        @Override
        void keyPressed(int key, int scancode, int modifiers) {
            if (focusedBox == null) {
                if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                    addAndFocus();
                }

                return;
            }

            final int index = boxes.indexOf(focusedBox);

            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (index < boxes.size() - 1) {
                    focus(boxes.get(index + 1));
                }
                else if (focusedBox.getValue().isBlank() || atLimit()) {
                    close();
                }
                else {
                    addAndFocus();
                }

                return;
            }

            if (key == GLFW.GLFW_KEY_BACKSPACE && focusedBox.getValue().isEmpty() && boxes.size() > owner.minSize()) {
                removeAt(index);
                return;
            }

            if (key == GLFW.GLFW_KEY_UP && index > 0) {
                focus(boxes.get(index - 1));
                return;
            }

            if (key == GLFW.GLFW_KEY_DOWN && index < boxes.size() - 1) {
                focus(boxes.get(index + 1));
                return;
            }

            focusedBox.keyPressed(key, scancode, modifiers);
        }

        @Override
        void charTyped(char c, int modifiers) {
            if (focusedBox != null) {
                focusedBox.charTyped(c, modifiers);
            }

        }

    }

    static final class RecordEditor extends ConfigPopup {

        private static final int ROW = 20;
        private static final int FIELD = 56;
        private static final int VISIBLE_ROWS = 8;

        private final RecordButton recordButton;
        private final List<StyledEditBox> boxes = new ArrayList<>();
        private final String[] labels;
        private final String[] ranges;
        private final int labelWidth;
        private final int rangeWidth;
        private final Scroller scroller = new Scroller();
        private StyledEditBox focusedBox;

        private int listTop;
        private int listBottom;

        RecordEditor(RecordButton owner) {
            super(owner);
            this.recordButton = owner;

            final int count = owner.components.length;
            this.labels = new String[count];
            this.ranges = new String[count];

            int widestLabel = 0;
            int widestRange = 0;
            for (int i = 0; i < count; i++) {
                final RecordComponent component = owner.components[i];
                final ConfigRange range = component.getAnnotation(ConfigRange.class);
                labels[i] = componentLabel(component.getName());
                ranges[i] = range != null && Double.isFinite(range.min()) && Double.isFinite(range.max()) ? displayLabel(range.min(), false) + " - " + displayLabel(range.max(), false) : "";
                widestLabel = Math.max(widestLabel, font.width(labels[i]));
                widestRange = Math.max(widestRange, font.width(ranges[i]));

                final int index = i;
                final StyledEditBox box = new StyledEditBox(font, 0, 0, FIELD, 16, Component.empty(), accent, false);
                box.setMaxLength(24);
                box.setValue(i < owner.values().size() ? displayLabel(owner.values().get(i), false) : "");
                box.setValidator(raw -> parse(index, raw) != null);
                box.setResponder(changed -> push(index));
                boxes.add(box);
            }

            this.labelWidth = widestLabel;
            this.rangeWidth = widestRange;
        }

        private Object parse(int index, String raw) {
            final String text = raw.trim();
            if (text.isEmpty()) {
                return null;
            }

            final RecordComponent component = recordButton.components[index];
            final ConfigRange range = component.getAnnotation(ConfigRange.class);
            final Class<?> type = component.getType();
            try {
                final Object value;
                if (type == int.class || type == Integer.class) {
                    value = Integer.parseInt(text);
                }
                else if (type == long.class || type == Long.class) {
                    value = Long.parseLong(text);
                }
                else if (type == float.class || type == Float.class) {
                    value = Float.parseFloat(text);
                }
                else if (type == double.class || type == Double.class) {
                    value = Double.parseDouble(text);
                }
                else {
                    return null;
                }

                final double number = ((Number) value).doubleValue();
                return Double.isNaN(number) || (range != null && (number < range.min() || number > range.max())) ? null : value;
            }
            catch (NumberFormatException exception) {
                return null;
            }

        }

        private void push(int index) {
            final Object value = parse(index, boxes.get(index).getValue());
            final List<Object> values = new ArrayList<>(recordButton.values());
            if (value != null && index < values.size()) {
                values.set(index, value);
                recordButton.setValues(values);
            }

        }

        private void focus(StyledEditBox box) {
            for (StyledEditBox other : boxes) {
                other.setFocused(other == box);
            }

            focusedBox = box;
            final int index = boxes.indexOf(box);
            if (index >= 0) {
                scroller.reveal(index * ROW, index * ROW + ROW, Math.max(ROW, listBottom - listTop));
            }

        }

        @Override
        int desiredWidth() {
            return Mth.clamp(PAD * 2 + 5 + labelWidth + 12 + (rangeWidth > 0 ? rangeWidth + 6 : 0) + FIELD + 4, 150, 280);
        }

        @Override
        int desiredHeight() {
            return PAD * 2 + Math.min(boxes.size(), VISIBLE_ROWS) * ROW;
        }

        @Override
        void renderContent(GuiGraphics graphics, int mx, int my, int top, int bottom) {
            final long now = Util.getMillis();

            listTop = top + PAD;
            listBottom = bottom - PAD;
            final int viewport = Math.max(0, listBottom - listTop);
            final int content = boxes.size() * ROW;
            scroller.update(content, viewport);

            final int rowLeft = x + PAD;
            final int rowRight = x + width - PAD - (content > viewport ? 4 : 0);
            final int fieldX = rowRight - FIELD;

            KnightLibConfigScreen.enableScissor(graphics, x + 1, listTop, x + width - 1, listBottom);

            for (int i = 0; i < boxes.size(); i++) {
                final StyledEditBox box = boxes.get(i);
                final int rowY = listTop + i * ROW - (int) scroller.scroll;

                box.setX(fieldX);
                box.setY(rowY + 2);
                box.setWidth(FIELD);

                if (rowY + ROW < listTop || rowY > listBottom) {
                    continue;
                }

                final float appear = ease((now - openedAt - Math.min(i, 8) * 18L) / 200f);
                graphics.pose().pushPose();
                graphics.pose().translate((1f - appear) * -6f, 0f, 0f);

                final int textY = rowY + (ROW - font.lineHeight) / 2 + 1;
                int labelRight = fieldX - 6;
                if (!ranges[i].isEmpty()) {
                    final boolean valid = parse(i, box.getValue()) != null;
                    labelRight -= font.width(ranges[i]);
                    graphics.drawString(font, ranges[i], labelRight, textY, opaque(valid ? TEXT_FAINT : ERROR), false);
                    labelRight -= 6;
                }

                graphics.drawString(font, ellipsize(font, labels[i], labelRight - rowLeft - 5), rowLeft + 5, textY, opaque(box.isFocused() ? TEXT : TEXT_SOFT), false);
                box.render(graphics, mx, my, 0f);

                graphics.pose().popPose();
            }

            scroller.renderEdges(graphics, x + 1, rowRight, listTop, listBottom, content, mx, my);
            graphics.disableScissor();
        }

        @Override
        boolean mouseClicked(double mx, double my, int button) {
            if (!isInside(mx, my)) {
                return false;
            }

            if (scroller.mouseClicked(mx, my)) {
                return true;
            }

            if (my >= listTop && my < listBottom) {
                for (StyledEditBox box : boxes) {
                    if (box.isMouseOver(mx, my)) {
                        focus(box);
                        box.mouseClicked(mx, my, button);
                        return true;
                    }

                }

                final int index = (int) ((my - listTop + scroller.scroll) / ROW);
                if (index >= 0 && index < boxes.size()) {
                    focus(boxes.get(index));
                }

            }

            return true;
        }

        @Override
        void mouseDragged(double mx, double my, int button, double dx, double dy) {
            if (!scroller.mouseDragged(my) && focusedBox != null) {
                focusedBox.mouseDragged(mx, my, button, dx, dy);
            }

        }

        @Override
        void mouseReleased(double mx, double my, int button) {
            scroller.mouseReleased();
        }

        @Override
        void mouseScrolled(double mx, double my, double scrollY) {
            scroller.scrollBy(-scrollY * ROW);
        }

        @Override
        void tick() {
            for (StyledEditBox box : boxes) {
                box.tick();
            }

        }

        @Override
        void keyPressed(int key, int scancode, int modifiers) {
            if (focusedBox == null) {
                if (!boxes.isEmpty() && (key == GLFW.GLFW_KEY_TAB || key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
                    focus(boxes.get(0));
                }

                return;
            }

            final int index = boxes.indexOf(focusedBox);
            final boolean back = key == GLFW.GLFW_KEY_UP || (key == GLFW.GLFW_KEY_TAB && (modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
            final boolean next = key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_TAB;

            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (index < boxes.size() - 1) {
                    focus(boxes.get(index + 1));
                }
                else {
                    close();
                }

                return;
            }

            if (back) {
                if (index > 0) {
                    focus(boxes.get(index - 1));
                }

                return;
            }

            if (next) {
                if (index < boxes.size() - 1) {
                    focus(boxes.get(index + 1));
                }

                return;
            }

            focusedBox.keyPressed(key, scancode, modifiers);
        }

        @Override
        void charTyped(char c, int modifiers) {
            if (focusedBox != null) {
                focusedBox.charTyped(c, modifiers);
            }

        }

    }

}