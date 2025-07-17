package dev.xylonity.knightlib.api;

import dev.xylonity.knightlib.api.impl.BossBarApi;
import dev.xylonity.knightlib.api.impl.CustomBossBarRenderer;
import net.minecraft.client.gui.components.LerpingBossEvent;

import java.util.function.Predicate;

public final class BossBarBuilder {

    private Predicate<LerpingBossEvent> matcher;
    private CustomBossBarRenderer renderer;
    private int padding = 0;
    private boolean hideName = false;

    /**
     * Literal name (not registry name) of the entity to apply.
     */
    public static BossBarBuilder matcher(Predicate<LerpingBossEvent> matcher) {
        BossBarBuilder builder = new BossBarBuilder();
        builder.matcher = matcher;
        return builder;
    }

    /**
     * Custom rendering logic of the actual boss bar and its derivative components.
     */
    public BossBarBuilder renderer(CustomBossBarRenderer renderer) {
        this.renderer = renderer;
        return this;
    }

    /**
     * Extra padding to apply between boss bars. The bar height (px) is recommended.
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
        BossBarApi.register(new BossBarApi.BossBarEntry(matcher, renderer, padding, hideName));
    }

}
