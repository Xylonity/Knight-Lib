package dev.xylonity.knightlib.api.tooltip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Tooltip helper for building multiline tooltips with indentation, styling, icons and text
 * wrapping (that approximately balances line breaks).
 * Code example:
 * <pre>
 *     {@code
 *         TooltipBuilder builder = TooltipBuilder.create()
 *              .icon(TooltipBuilder.CustomIcon.STAR).literal(" ")
 *              .key("tooltip.item.companions.key.blood_set", Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF00)))
 *              .newLine().indent()
 *              .literal("Abilities", ChatFormatting.DARK_GRAY)
 *              .newLine().indent()
 *              .literal("Blood crystals")
 *              .newLine().indent()
 *              .wrapKey("tooltip.knightlib.long_description", 6, ChatFormatting.GRAY, 20)
 *              .newLine().unindent()
 *              .literal("Reduction")
 *              .newLine().indent()
 *              .key("tooltip.knightlib.description", ChatFormatting.RED, 10);
 *
 *          tooltip.addAll(builder.build());
 *     }
 * </pre>
 */
public class TooltipBuilder {

    /**
     * Contains every component representing each tooltip line
     */
    private final List<Component> lines = new ArrayList<>();
    private MutableComponent current = Component.literal("");
    private int indent = 0;

    public static TooltipBuilder create() {
        return new TooltipBuilder();
    }

    /**
     * Jumps to the next line
     */
    public TooltipBuilder newLine() {
        if (hasContent()) {
            clear();
        } else {
            lines.add(Component.literal(""));
        }

        return this;
    }

    /**
     * Inserts a blank line regardless of the buffer. newLine() is preferred for almost any use case
     */
    public TooltipBuilder blankLine() {
        lines.add(Component.literal(""));
        return this;
    }

    /**
     * Increases indentation by 1
     */
    public TooltipBuilder indent() {
        indent++;
        return this;
    }

    /**
     * Decreases indentation by 1 (minimum 0)
     */
    public TooltipBuilder unindent() {
        if (indent > 0) {
            indent--;
        }

        return this;
    }

    /**
     * Appends a translatable text component with certain args
     */
    public TooltipBuilder key(String key, Object... args) {
        current.append(Component.translatable(key, args));
        return this;
    }

    /**
     * Appends a translatable text component with certain args and a certain style
     */
    public TooltipBuilder key(String key, ChatFormatting color, Object... args) {
        current.append(Component.translatable(key, args).withStyle(color));
        return this;
    }

    /**
     * Appends a translatable text component with certain args and a certain style
     */
    public TooltipBuilder key(String key, Style style, Object... args) {
        current.append(Component.translatable(key, args).withStyle(style));
        return this;
    }

    /**
     * Appends a literal component (non-translatable) with certain styling
     */
    public TooltipBuilder literal(String text, ChatFormatting... args) {
        MutableComponent component = Component.literal(text);
        for (ChatFormatting x : args) {
            component.withStyle(x);
        }

        current.append(component);
        return this;
    }

    /**
     * Appends a predefined icon (from this lib's registrated atlas fonts)
     */
    public TooltipBuilder icon(CustomIcon icon) {
        current.append(icon.component());
        return this;
    }

    /**
     * Wraps a translatable text into multiple lines, limited by maxWordPerLine, automatically
     * balancing the line lengths
     */
    public TooltipBuilder wrapKey(String key, int maxWordsPerLine, ChatFormatting style, Object... args) {
        String text = Component.translatable(key, args).getString();
        String[] words = text.split("\\s+");
        int wordCount = words.length;

        if (wordCount == 0) return this;

        int remainingWords = wordCount;
        int remainingChars = text.length();

        int idx = 0;
        // Greedy algorithm adaptation
        // https://github.com/apache/commons-text/blob/master/src/main/java/org/apache/commons/text/WordUtils.java
        while (idx < wordCount) {
            int linesLeft = (remainingWords + maxWordsPerLine - 1) / maxWordsPerLine;
            int aproxLength = remainingChars / linesLeft;

            StringBuilder builder = new StringBuilder();
            int wordsInLine = 0;

            // Word accumulator until hitting the aproximated length
            while (idx < wordCount && wordsInLine < maxWordsPerLine) {
                String word = words[idx];
                int line = builder.length() + (builder.isEmpty() ? 0 : 1) + word.length();

                // if adding this word would exceed the line length and we already have
                // at least one word, line break here we go
                if (wordsInLine > 0 && line > aproxLength) break;

                if (!builder.isEmpty()) builder.append(' ');

                builder.append(word);

                idx++;
                wordsInLine++;
                remainingWords--;
            }

            // account for characters used
            remainingChars -= builder.length();

            current.append(Component.literal(builder.toString()).withStyle(style));

            // if more word remain, flush to start a new line buffer
            if (idx < wordCount) clear();
        }

        return this;
    }

    /**
     * Wraps a translatable text into multiple lines, limited by maxWordPerLine, automatically
     * balancing the line lengths. Uses gray color.
     */
    public TooltipBuilder wrapKey(String key, int maxWords, Object... a) {
        return wrapKey(key, maxWords, ChatFormatting.GRAY, a);
    }

    /**
     * Wraps a translatable text AND styles it with some funny nyan cat ahh effect
     */
    public TooltipBuilder rainbow(String key, RainbowMode type, long cycleTime, int color1, int color2, Object... a) {
        String text = Component.translatable(key, a).getString();
        long time = System.currentTimeMillis();
        long period = cycleTime <= 0 ? 1 : cycleTime;
        for (int i = 0; i < text.length(); i++) {
            int rgb;
            switch (type) {
                // Adaptation from:
                // https://docs.oracle.com/javase/8/docs/api/java/awt/Color.html#HSBtoRGB
                case GRADIENT -> {
                    float t = (float) i / (text.length() - 1);
                    int r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
                    int r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;
                    int r = r1 + Math.round((r2 - r1) * t);
                    int g = g1 + Math.round((g2 - g1) * t);
                    int b = b1 + Math.round((b2 - b1) * t);
                    rgb = (r << 16) | (g << 8) | b;
                }
                case PULSE -> {
                    float n = 0.5f * (1 + (float) Math.sin(2 * Math.PI * ((time % period) / (float) period)));
                    rgb = lerpColor(color1, color2, n);
                }
                case BLINK -> {
                    float phase = ((time % period) / (float) period);
                    if (Math.abs(Math.sin(2 * Math.PI * (i + phase * 10))) > 0.9f) {
                        rgb = 0xFFFFFF;
                    } else {
                        rgb = lerpColor(color1, color2, phase);
                    }
                }
                case FADE -> {
                    float n = (((time % period) / (float) period) + (float) i / text.length()) % 1f;
                    if (n > 0.5f) {
                        n = 1f - n;
                    }

                    rgb = lerpColor(color1, color2, n * 2f);
                }
                case WAVE -> {
                    float hue = (((time % period) / (float) period) + 0.5f * (1 + (float) Math.sin(2 * Math.PI * i / text.length()))) % 1f;
                    rgb = Color.HSBtoRGB(hue, 1f, 1f);
                }
                case SWITCH -> {
                    rgb = ((time % period) < (period / 2)) ? color1 : color2;
                }
                default -> { // SCROLL
                    float hue = (((time % period) / (float) period) + (float) i / text.length()) % 1f;
                    rgb = Color.HSBtoRGB(hue, 1f, 1f);
                }
            }

            current.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
        }

        return this;
    }

    /**
     * Checks if the current buffer has any text appended
     */
    private boolean hasContent() {
        return !current.getString().isEmpty() || !current.getSiblings().isEmpty();
    }

    private static int lerpColor(int color1, int color2, float norm) {
        int r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;
        int r = r1 + Math.round((r2 - r1) * norm);
        int g = g1 + Math.round((g2 - g1) * norm);
        int b = b1 + Math.round((b2 - b1) * norm);
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Applies a line break and resets the buffer
     */
    private void clear() {
        if (hasContent()) {
            lines.add(applyIndent(current));
        }

        current = Component.literal("");
    }

    /**
     * Helper that returns a component with the applied indentations
     */
    private Component applyIndent(Component component) {
        return indent > 0 ? Component.literal(" ".repeat(indent)).append(component) : component;
    }

    public List<Component> build() {
        clear();
        return List.copyOf(lines);
    }

    public enum CustomIcon {
        SHIFT, TICK, STAR, INFO, CROSS, SKULL, BLUE_BALL, LOCATION, HOURGLASS;

        public Component component() {
            return Component.translatable("tooltip.icon.knightlib." + name().toLowerCase());
        }
    }

    /**
     * Unused currently
     */
    public enum GradientDir {
        HORIZONTAL, VERTICAL
    }

    public enum RainbowMode {
        SCROLL, GRADIENT, WAVE, PULSE, SWITCH, BLINK, FADE
    }

}