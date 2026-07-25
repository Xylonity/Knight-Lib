package dev.xylonity.knightlib.common.item.blockitem;

import dev.xylonity.knightlib.api.animation.KnightLibItemAnimations;
import dev.xylonity.knightlib.api.item.KnightLibRenderedItem;
import dev.xylonity.knightlib.client.item.GenericBlockItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Generic block item rendered with KnightLib's item renderer.
 *
 * The renderer resolves {@code geo/<path>.geo.json} and {@code textures/block/<path>.png} in the namespace supplied by {@code rendererId}.
 */
public class GenericRenderedBlockItem extends BlockItem implements KnightLibRenderedItem {

    private final ResourceLocation rendererId;

    public GenericRenderedBlockItem(Block block, Properties properties, ResourceLocation rendererId) {
        super(block, properties);
        this.rendererId = Objects.requireNonNull(rendererId, "rendererId");
    }

    public ResourceLocation rendererId() {
        return rendererId;
    }

    /**
     * Stub that matches forge's optional item hook
     */
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return KnightLibItemAnimations.shouldCauseReequipAnimation(oldStack, newStack);
    }

    /**
     * Client-only factory used by the loader adapters when this item first needs to be rendered.
     *
     * When using this, create a synthetic method (() -> ()), don't call the lambda directly, so
     * no client-sided classes are loaded on the server.
     */
    @Override
    public Supplier<Object> rendererFactory() {
        return () -> new GenericBlockItemRenderer(rendererId);
    }

}
