package dev.xylonity.knightlib.client.animation;

import dev.xylonity.knightlib.api.animation.KnightLibAnimatable;
import dev.xylonity.knightlib.network.packets.AnimationSyncS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Client end of server-triggered animations.
 *
 * Based off Citadel implementation
 * https://github.com/AlexModGuy/Citadel/blob/1.20/src/main/java/com/github/alexthe666/citadel/server/message/AnimationMessage.java
 * https://github.com/AlexModGuy/Citadel/blob/1.20/src/main/java/com/github/alexthe666/citadel/ClientProxy.java
 */
public final class AnimationSyncClient {

    private static final Deque<Pending> PENDING = new ArrayDeque<>();

    public static void handle(AnimationSyncS2C message) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        if (apply(level, message)) {
            // A newer animation command can resolve after an older one was queued before the target's spawn packet
            discardPendingStream(message);
            return;
        }

        discardPendingStream(message);
        if (PENDING.size() >= 1024) {
            PENDING.removeFirst();
        }

        PENDING.addLast(new Pending(message, level.getGameTime() + 200));
    }

    /**
     * Retries packets that arrived before their entity/chunk spawn packet
     */
    public static void tick() {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            PENDING.clear();
            return;
        }

        final long now = level.getGameTime();
        final Iterator<Pending> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            final Pending pending = iterator.next();
            if (now > pending.expiresAt() || apply(level, pending.message())) {
                iterator.remove();
            }

        }

    }

    public static void clear() {
        PENDING.clear();
    }

    private static void discardPendingStream(AnimationSyncS2C message) {
        PENDING.removeIf(pending -> sameStream(pending.message(), message));
    }

    private static boolean sameStream(AnimationSyncS2C first, AnimationSyncS2C second) {
        if (first.entityTarget() != second.entityTarget() || !first.controller().equals(second.controller())) {
            return false;
        }

        return first.entityTarget()
                ? first.entityId() == second.entityId()
                : first.pos().equals(second.pos());
    }

    private static boolean apply(ClientLevel level, AnimationSyncS2C message) {
        KnightLibAnimatable target = null;
        if (message.entityTarget()) {
            final Entity entity = level.getEntity(message.entityId());
            if (entity instanceof KnightLibAnimatable animatable) {
                target = animatable;
            }

        }
        else {
            final BlockEntity blockEntity = level.getBlockEntity(message.pos());
            if (blockEntity instanceof final KnightLibAnimatable animatable) {
                target = animatable;
            }

        }

        if (target != null) {
            target.getAnimationHandler().applyRemote(message.controller(), message.steps(),
                    message.transitionTicks(), message.easingId(), message.speed(),
                    message.commandGameTime(), message.blendMode(), message.snapshot(), message.mask(), message.weight()
            );

            return true;
        }

        return false;
    }

    private record Pending(
            AnimationSyncS2C message,
            long expiresAt
    ) {
        ;;
    }

}