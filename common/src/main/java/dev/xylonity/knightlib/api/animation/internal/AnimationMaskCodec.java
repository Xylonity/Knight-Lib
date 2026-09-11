package dev.xylonity.knightlib.api.animation.internal;

import dev.xylonity.knightlib.api.animation.KnightLibAnimationMask;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashSet;
import java.util.Set;

public final class AnimationMaskCodec {

    public static void write(KnightLibAnimationMask mask, FriendlyByteBuf buffer) {
        buffer.writeBoolean(mask.allBones());
        buffer.writeByte(mask.channelBits());
        buffer.writeVarInt(mask.boneNames().size());
        for (final String bone : mask.boneNames()) {
            buffer.writeUtf(bone, 256);
        }

    }

    public static KnightLibAnimationMask read(FriendlyByteBuf buffer) {
        final boolean all = buffer.readBoolean();
        final int channels = buffer.readUnsignedByte();
        final int count = buffer.readVarInt();
        if (count < 0 || count > KnightLibAnimationMask.MAX_BONES) {
            throw new IllegalArgumentException("[KnightLib] Invalid animation mask size");
        }

        final Set<String> bones = new HashSet<>();
        for (int i = 0; i < count; i++) {
            bones.add(buffer.readUtf(256));
        }

        return new KnightLibAnimationMask(bones, channels, all);
    }

    public static void write(KnightLibAnimationMask mask, CompoundTag tag) {
        tag.putBoolean("MaskAllBones", mask.allBones());
        tag.putInt("MaskChannels", mask.channelBits());
        final ListTag bones = new ListTag();
        for (final String bone : mask.boneNames()) {
            bones.add(StringTag.valueOf(bone));
        }

        tag.put("MaskBones", bones);
    }

    public static KnightLibAnimationMask read(CompoundTag tag) {
        if (!tag.contains("MaskChannels", Tag.TAG_ANY_NUMERIC)) {
            return KnightLibAnimationMask.ALL;
        }

        final ListTag names = tag.getList("MaskBones", Tag.TAG_STRING);
        if (names.size() > KnightLibAnimationMask.MAX_BONES) {
            throw new IllegalArgumentException("[KnightLib] Invalid animation mask size");
        }

        final Set<String> bones = new HashSet<>();
        for (int i = 0; i < names.size(); i++) {
            bones.add(names.getString(i));
        }

        return new KnightLibAnimationMask(bones, tag.getInt("MaskChannels"), tag.getBoolean("MaskAllBones"));
    }

}