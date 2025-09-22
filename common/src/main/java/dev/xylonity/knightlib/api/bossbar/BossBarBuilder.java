package dev.xylonity.knightlib.api.bossbar;

import dev.xylonity.knightlib.impl.internal.BossBarApi;
import dev.xylonity.knightlib.impl.internal.LegacyCustomBossBarRenderer;
import dev.xylonity.knightlib.impl.internal.CustomBossBarRenderer;
import net.minecraft.client.gui.components.LerpingBossEvent;

import java.util.function.Predicate;

/**
 * Client-sided abstraction. The boss bar instances should be registered on the client MOD bus on the forge loader or either
 * inside onInitializeClient on fabric. Use either legacyMatching or normal matching, don't mix implementations.
 */
public final class BossBarBuilder {

    private Predicate<LerpingBossEvent> legacyMatcher;
    private Predicate<BossBarContext> matcher;
    private LegacyCustomBossBarRenderer legacyRenderer;
    private CustomBossBarRenderer renderer;
    private int padding = 0;
    private boolean hideName = false;

    /**
     * Literal name (not registry name) of the entity to apply.
     * @param matcher the string to match
     */
    public static BossBarBuilder legacyMatcher(Predicate<LerpingBossEvent> matcher) {
        BossBarBuilder builder = new BossBarBuilder();
        builder.legacyMatcher = matcher;
        return builder;
    }

    /**
     * Contains the literal entity to apply. This is a custom implementation that reproduces a custom bossbar
     * using a BossBarContext as a bridge, that contains, through networking, the actual entity.
     *
     * @see TrackedServerBossEvent
     * @param matcher the string to match
     */
    public static BossBarBuilder matcher(Predicate<BossBarContext> matcher) {
        BossBarBuilder builder = new BossBarBuilder();
        builder.matcher = matcher;
        return builder;
    }

    /**
     * Custom rendering logic of the actual boss bar and its derivative components.
     * @param renderer the renderer. Receives 4 params, as declared inside the abstract method of LegacyCustomBossBarRenderer
     */
    public BossBarBuilder legacyRenderer(LegacyCustomBossBarRenderer renderer) {
        this.legacyRenderer = renderer;
        return this;
    }

    /**
     * Custom rendering logic of the actual boss bar and its derivative components.
     * @param renderer the renderer. Receives 4 params, as declared inside the abstract method of CustomBossBarRenderer
     */
    public BossBarBuilder renderer(CustomBossBarRenderer renderer) {
        this.renderer = renderer;
        return this;
    }

    /**
     * Padding to apply between boss bars. The bar height (px) is recommended.
     * @param padding the padding to apply
     */
    public BossBarBuilder padding(int padding) {
        this.padding = padding;
        return this;
    }

    public BossBarBuilder hideVanillaName() {
        this.hideName = true;
        return this;
    }

    public void register() {
        BossBarApi.register(new BossBarApi.BossBarEntry(legacyMatcher, matcher, legacyRenderer, renderer, padding, hideName));
    }

}