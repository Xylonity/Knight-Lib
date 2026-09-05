package dev.xylonity.knightlib.network.packets;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.animation.KnightLibAnim;
import dev.xylonity.knightlib.api.animation.KnightLibAnimationBlendMode;
import dev.xylonity.knightlib.api.animation.KnightLibAnimationMask;
import dev.xylonity.knightlib.api.animation.internal.AnimationMaskCodec;
import dev.xylonity.knightlib.network.ClientboundPacketType;
import dev.xylonity.knightlib.network.ClientPacketDispatcher;
import dev.xylonity.knightlib.network.PacketCodec;
import dev.xylonity.knightlib.network.PacketType;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Clientbound packet that plays or stops an animation controller on a tracked entity or block entity. An empty
 * step list means stop.
 */
public record AnimationSyncS2C(
        boolean entityTarget,
        int entityId,
        BlockPos pos,
        String controller,
        List<KnightLibAnim.Step> steps,
        int transitionTicks,
        int easingId,
        float speed,
        long commandGameTime,
        KnightLibAnimationBlendMode blendMode,
        boolean snapshot,
        KnightLibAnimationMask mask,
        float weight
) {

    public AnimationSyncS2C(boolean entityTarget, int entityId, BlockPos pos, String controller, List<KnightLibAnim.Step> steps, int transitionTicks, int easingId, float speed, long commandGameTime, KnightLibAnimationBlendMode blendMode) {
        this(entityTarget, entityId, pos, controller, steps, transitionTicks, easingId, speed, commandGameTime, blendMode, false);
    }

    public AnimationSyncS2C(boolean entityTarget, int entityId, BlockPos pos, String controller, List<KnightLibAnim.Step> steps, int transitionTicks, int easingId, float speed, long commandGameTime, KnightLibAnimationBlendMode blendMode, boolean snapshot) {
        this(entityTarget, entityId, pos, controller, steps, transitionTicks, easingId, speed, commandGameTime, blendMode, snapshot, KnightLibAnimationMask.ALL, 1f);
    }

    public static final ResourceLocation ID = new ResourceLocation(KnightLib.MOD_ID, "animation_sync");

    public AnimationSyncS2C {
        steps = List.copyOf(steps);
        if (!KnightLibAnim.isValidSequence(steps)) {
            throw new IllegalArgumentException("Invalid animation sequence");
        }

        Objects.requireNonNull(blendMode, "blendMode");
        Objects.requireNonNull(mask, "mask");
        if (!Float.isFinite(weight) || weight < 0f || weight > 1f) {
            throw new IllegalArgumentException("[KnightLib] Invalid animation weight");
        }

    }

    public static final ClientboundPacketType<AnimationSyncS2C> TYPE =
            PacketType.clientbound(
                    ID,
                    AnimationSyncS2C.class,
                    PacketCodec.of(AnimationSyncS2C::encode, AnimationSyncS2C::decode),
                    ClientPacketDispatcher::dispatch
            );

    public static void encode(AnimationSyncS2C packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.entityTarget());
        if (packet.entityTarget()) {
            buf.writeVarInt(packet.entityId());
        }
        else {
            buf.writeBlockPos(packet.pos());
        }

        buf.writeUtf(packet.controller(), 64);
        buf.writeVarInt(packet.steps().size());
        for (final KnightLibAnim.Step step : packet.steps()) {
            buf.writeUtf(step.animation(), 256);
            buf.writeVarInt(step.mode().id());
        }

        buf.writeVarInt(packet.transitionTicks());
        buf.writeVarInt(packet.easingId());
        buf.writeFloat(packet.speed());
        buf.writeVarInt(packet.blendMode().id());
        buf.writeLong(packet.commandGameTime());
        buf.writeBoolean(packet.snapshot());
        buf.writeFloat(packet.weight());
        AnimationMaskCodec.write(packet.mask(), buf);
    }

    public static AnimationSyncS2C decode(FriendlyByteBuf buf) {
        final boolean entityTarget = buf.readBoolean();
        int entityId = 0;
        BlockPos pos = BlockPos.ZERO;
        if (entityTarget) {
            entityId = buf.readVarInt();
        }
        else {
            pos = buf.readBlockPos();
        }

        final String controller = buf.readUtf(64);
        final int stepCount = buf.readVarInt();
        if (stepCount < 0 || stepCount > KnightLibAnim.MAX_STEPS) {
            throw new DecoderException("[KnightLib] Invalid animation sequence size");
        }

        final List<KnightLibAnim.Step> steps = new ArrayList<>(stepCount);
        try {
            for (int i = 0; i < stepCount; i++) {
                steps.add(new KnightLibAnim.Step(buf.readUtf(256), KnightLibAnim.PlaybackMode.byId(buf.readVarInt())));
            }

        }
        catch (IllegalArgumentException exception) {
            throw new DecoderException("[KnightLib] Invalid animation sequence step", exception);
        }

        final int transition = buf.readVarInt();
        final int easing = buf.readVarInt();
        final float speed = buf.readFloat();
        final KnightLibAnimationBlendMode blendMode;
        try {
            blendMode = KnightLibAnimationBlendMode.byId(buf.readVarInt());
        }
        catch (IllegalArgumentException exception) {
            throw new DecoderException("[KnightLib] Invalid animation blend mode", exception);
        }

        final long commandGameTime = buf.readLong();
        if (controller.isBlank() || transition < 0 || transition > 1200 || !Float.isFinite(speed) || speed <= 0f || speed > 100f || commandGameTime < 0L || !KnightLibAnim.isValidSequence(steps)) {
            throw new DecoderException("[KnightLib] Invalid animation sync payload");
        }

        final boolean snapshot = buf.isReadable() && buf.readBoolean();
        float weight = 1f;
        KnightLibAnimationMask mask = KnightLibAnimationMask.ALL;
        try {
            if (buf.isReadable()) {
                weight = buf.readFloat();
                mask = AnimationMaskCodec.read(buf);
            }

            return new AnimationSyncS2C(entityTarget, entityId, pos, controller, steps, transition, easing, speed, commandGameTime, blendMode, snapshot, mask, weight);
        }
        catch (Exception exception) {
            throw new DecoderException("[KnightLib] Invalid animation mask or weight", exception);
        }

    }

}