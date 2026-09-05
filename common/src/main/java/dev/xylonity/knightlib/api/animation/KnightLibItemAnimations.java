package dev.xylonity.knightlib.api.animation;

import dev.xylonity.knightlib.api.item.KnightLibAnimatedItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Backing store for per-stack animation handlers. Normal callers should use {@link KnightLibAnimatedItem}, since this class
 * mostly exists to keep an item's instance separate from the state of each physical stack.
 *
 * Based off GeckoLib implementation
 * https://github.com/bernie-g/geckolib/blob/1.20.1/core/src/main/java/software/bernie/geckolib/core/animatable/instance/SingletonAnimatableInstanceCache.java
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/cache/AnimatableIdCache.java
 * https://github.com/bernie-g/geckolib/blob/1.20.1/Forge/src/main/java/software/bernie/geckolib/animatable/SingletonGeoAnimatable.java
 */
public final class KnightLibItemAnimations {

    static final String TAG = "KnightLibAnimations";

    private static final ListTag EMPTY_STATE = new ListTag();

    private static final Map<ItemStack, CacheEntry> HANDLERS = new WeakHashMap<>();


    public static KnightLibAnimationHandler getAnimationHandler(ItemStack stack, @Nullable Level level) {
        return getAnimationHandler(animatedItem(stack), stack, level, null);
    }

    /**
     * Direct lookup used by {@link KnightLibAnimatedItem}
     */
    public static KnightLibAnimationHandler getAnimationHandler(KnightLibAnimatedItem item, ItemStack stack, @Nullable Level level) {
        return getAnimationHandler(item, stack, level, null);
    }

    /**
     * Context-aware lookup used by the automatic ItemStack tick hook
     */
    public static KnightLibAnimationHandler getAnimationHandler(ItemStack stack, @Nullable Level level, @Nullable Entity holder) {
        return getAnimationHandler(animatedItem(stack), stack, level, holder);
    }

    /**
     * Context-aware lookup used by the automatic ItemStack tick hook
     */
    public static KnightLibAnimationHandler getAnimationHandler(KnightLibAnimatedItem item, ItemStack stack, @Nullable Level level, @Nullable Entity holder) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(stack, "stack");
        if (item instanceof Item vanillaItem && stack.getItem() != vanillaItem) {
            throw new IllegalArgumentException("[KnightLib] The ItemStack does not contain the supplied KnightLibAnimatedItem");
        }

        final ListTag state = state(stack);
        synchronized (HANDLERS) {
            final CacheEntry entry = HANDLERS.get(stack);
            if (entry == null) {
                final KnightLibAnimationHandler handler = KnightLibAnimationHandler.of(item, stack, level);
                handler.updateItemContext(stack, level, holder);
                handler.load(wrap(state));

                HANDLERS.put(stack, new CacheEntry(handler, copy(state)));

                return handler;
            }

            entry.handler.updateItemContext(stack, level, holder);
            if (!entry.state.equals(state)) {
                entry.handler.load(wrap(state), false);
                entry.state = copy(state);
            }

            return entry.handler;
        }

    }

    /**
     * Whether server ticking is needed to expire a duration-capped animation or remove a completed stop transition.
     */
    public static boolean needsServerTick(ItemStack stack) {
        final CompoundTag ownerTag = stack.getTag();
        if (ownerTag == null || !ownerTag.contains(TAG, Tag.TAG_LIST)) {
            return false;
        }

        final ListTag controllers = ownerTag.getList(TAG, Tag.TAG_COMPOUND);
        if (controllers.isEmpty()) {
            return true;
        }

        for (int i = 0; i < controllers.size(); i++) {
            final CompoundTag controller = controllers.getCompound(i);
            final String animation = controller.getString("Animation");
            if (animation.isBlank() || controller.getInt("Duration") > 0) {
                return true;
            }

        }

        return false;
    }

    /**
     * Clears client-local handlers when the client changes world or disconnects.
     */
    public static void clear() {
        synchronized (HANDLERS) {
            HANDLERS.clear();
        }

    }

    /**
     * Compares held-item state while ignoring only KnightLib's controller snapshot, used for the forge's stub method, so now
     * animations no longer cause a hand pop, while item, count, capability and unrelated NBT changes retain the normal re-equip behavior.
     */
    public static boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack) {
        Objects.requireNonNull(oldStack, "oldStack");
        Objects.requireNonNull(newStack, "newStack");
        if (oldStack == newStack) {
            return false;
        }

        return !ItemStack.matches(withoutAnimationState(oldStack), withoutAnimationState(newStack));
    }

    private static ItemStack withoutAnimationState(ItemStack stack) {
        final ItemStack copy = stack.copy();
        final CompoundTag tag = copy.getTag();
        if (tag != null && tag.contains(TAG)) {
            tag.remove(TAG);
            if (tag.isEmpty()) {
                copy.setTag(null);
            }

        }

        return copy;
    }

    private static ListTag state(ItemStack stack) {
        final CompoundTag ownerTag = stack.getTag();
        if (ownerTag != null && ownerTag.contains(TAG, Tag.TAG_LIST)) {
            return ownerTag.getList(TAG, Tag.TAG_COMPOUND);
        }

        return EMPTY_STATE;
    }

    private static CompoundTag wrap(ListTag state) {
        final CompoundTag ownerTag = new CompoundTag();
        ownerTag.put(TAG, state);
        return ownerTag;
    }

    private static ListTag copy(ListTag state) {
        return state.copy();
    }

    private static KnightLibAnimatedItem animatedItem(ItemStack stack) {
        if (stack.getItem() instanceof KnightLibAnimatedItem item) {
            return item;
        }

        throw new IllegalArgumentException("[KnightLib] Item must implement KnightLibAnimatedItem: " + stack.getItem());
    }

    private static final class CacheEntry {

        private final KnightLibAnimationHandler handler;
        private ListTag state;

        private CacheEntry(KnightLibAnimationHandler handler, ListTag state) {
            this.handler = handler;
            this.state = state;
        }

    }

}