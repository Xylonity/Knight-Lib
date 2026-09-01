package dev.xylonity.knightlib.api.animation.internal;

import dev.xylonity.knightlib.api.animation.KnightLibAnimatable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Keeps animatable block entities ticking even when they do not have a vanilla be ticker
 */
public final class BlockEntityAnimationTicker {

    private static final Map<Level, Set<BlockEntity>> BY_LEVEL = new WeakHashMap<>();

    public static void register(BlockEntity blockEntity) {
        if (!(blockEntity instanceof KnightLibAnimatable) || blockEntity.getLevel() == null || blockEntity.isRemoved()) {
            return;
        }

        synchronized (BY_LEVEL) {
            // Keeping the same instance per dimension
            removeFromAll(blockEntity);
            BY_LEVEL.computeIfAbsent(blockEntity.getLevel(), ignored -> Collections.newSetFromMap(new WeakHashMap<>()))
                    .add(blockEntity);
        }

    }

    public static void unregister(BlockEntity blockEntity) {
        synchronized (BY_LEVEL) {
            removeFromAll(blockEntity);
        }

    }

    public static void tick(Level level) {
        final List<BlockEntity> snapshot;
        synchronized (BY_LEVEL) {
            final Set<BlockEntity> blockEntities = BY_LEVEL.get(level);
            if (blockEntities == null || blockEntities.isEmpty()) {
                return;
            }

            blockEntities.removeIf(blockEntity -> blockEntity == null || blockEntity.isRemoved() || blockEntity.getLevel() != level);
            snapshot = new ArrayList<>(blockEntities);
        }

        for (final BlockEntity blockEntity : snapshot) {
            if (blockEntity instanceof KnightLibAnimatable animatable) {
                animatable.tickAnimations();
            }

        }

    }

    public static void clear(Level level) {
        synchronized (BY_LEVEL) {
            BY_LEVEL.remove(level);
        }

    }

    private static void removeFromAll(BlockEntity target) {
        BY_LEVEL.values().removeIf(blockEntities -> {
            blockEntities.remove(target);
            return blockEntities.isEmpty();
        });

    }

}
