package dev.xylonity.knightlib.api.client.armor;

import dev.xylonity.knightlib.api.animation.KnightLibAnimationHandler;
import dev.xylonity.knightlib.api.animation.KnightLibItemAnimations;
import dev.xylonity.knightlib.api.client.animation.KnightLibAnimation;
import dev.xylonity.knightlib.api.item.KnightLibAnimatedItem;
import dev.xylonity.knightlib.client.animation.KnightLibAnimationSource;
import dev.xylonity.knightlib.client.animation.KnightLibAnimator;
import dev.xylonity.knightlib.client.animation.KnightLibPose;
import dev.xylonity.knightlib.client.animation.model.KnightLibModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side base for an equipped vanilla-pipeline armor renderer
 */
public abstract class KnightLibArmorRenderer {

    private KnightLibArmorModel model;
    private int cachedModelGeneration = Integer.MIN_VALUE;

    /**
     * Creates the model lazily, after client model layers are available
     */
    protected abstract KnightLibArmorModel createModel();

    /**
     * Generation of resources backing the model
     */
    protected int modelGeneration() {
        return 0;
    }

    /**
     * Animation assets used by this stack. Only accepts vanilla animations
     */
    protected KnightLibAnimationSource defineAnimations(ItemStack stack) {
        return KnightLibAnimationSource.none();
    }

    /**
     * Name of a looping animation driven by the game clock, or null for the rest pose (convenience)
     */
    protected @Nullable String getAmbientAnimation(ItemStack stack) {
        return null;
    }

    protected float getAmbientAnimationSpeed(ItemStack stack) {
        return 1f;
    }

    /**
     * Called every frame after animations are applied and before rendering
     */
    protected void setupPose(ItemStack stack, LivingEntity wearer, EquipmentSlot slot, KnightLibModel model, float partialTicks) {
    }

    /**
     * Called every frame after animations are applied and before rendering
     */
    protected void setupBone(ItemStack stack, LivingEntity wearer, EquipmentSlot slot, KnightLibModel model, String boneName, float partialTicks) {
    }

    /**
     * Texture used by vanilla's armor layer
     */
    public abstract ResourceLocation getTextureLocation(ItemStack stack, LivingEntity wearer, EquipmentSlot slot, @Nullable String layer);

    public final KnightLibArmorModel prepareModel(LivingEntity wearer, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> contextModel) {
        final int generation = modelGeneration();
        if (model == null || cachedModelGeneration != generation) {
            model = createModel();
            if (model == null) {
                throw new IllegalStateException("[KnightLib] armor renderer returned a null model");
            }

            cachedModelGeneration = generation;
        }

        final KnightLibModel animationModel = model.animationModel();
        final float partialTicks = Minecraft.getInstance().getFrameTime();
        final double now = wearer.level().getGameTime() + partialTicks;

        if (stack.getItem() instanceof KnightLibAnimatedItem animatedItem) {
            final KnightLibAnimationHandler handler = KnightLibItemAnimations.getAnimationHandler(animatedItem, stack, wearer.level(), wearer);
            handler.tick();

            if (!handler.controllers().isEmpty()) {
                final KnightLibAnimationSource animations = defineAnimations(stack);
                KnightLibAnimator.animate(handler, animationModel, animations::get, now);
            }
            else {
                applyAmbientOrRest(stack, animationModel, now);
            }

        }
        else {
            applyAmbientOrRest(stack, animationModel, now);
        }

        // Controllers produce a delta from the rest pose, so it is captured to not break other poses
        final KnightLibPose animatedPose = animationModel.capturePose();
        animationModel.resetPose();

        final KnightLibPose restPose = animationModel.capturePose();
        model.composeWithWearer(contextModel, animatedPose, restPose);

        model.prepareForSlot(slot);
        setupPose(stack, wearer, slot, animationModel, partialTicks);
        animationModel.forEachBone(bone -> setupBone(stack, wearer, slot, animationModel, bone, partialTicks));

        return model;
    }

    private void applyAmbientOrRest(ItemStack stack, KnightLibModel model, double now) {
        final String ambient = getAmbientAnimation(stack);
        final KnightLibAnimationSource animations = defineAnimations(stack);
        final KnightLibAnimation animation = ambient == null ? null : animations.get(ambient);
        if (animation == null) {
            model.resetPose();
        }
        else {
            KnightLibAnimator.applyAmbient(model, animation, now, getAmbientAnimationSpeed(stack));
        }

    }

}