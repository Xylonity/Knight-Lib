package dev.xylonity.knightlib.api.entity.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.UUID;
import java.util.function.Function;

/**
 * Bridges an attachment value {@code T} and its NBT representation.
 * <br><br>
 * Common primitive serializers are provided as constants. For non-trivial definitions, a function call is probably needed here
 *
 * @see AttachmentType
 */
public interface AttachmentSerializer<T> {

    Tag write(T value);

    T read(Tag tag);

    AttachmentSerializer<Integer> INT = of(IntTag::valueOf, tag -> ((NumericTag) tag).getAsInt());
    AttachmentSerializer<Long> LONG = of(LongTag::valueOf, tag -> ((NumericTag) tag).getAsLong());
    AttachmentSerializer<Float> FLOAT = of(FloatTag::valueOf, tag -> ((NumericTag) tag).getAsFloat());
    AttachmentSerializer<Double> DOUBLE = of(DoubleTag::valueOf, tag -> ((NumericTag) tag).getAsDouble());
    AttachmentSerializer<Boolean> BOOLEAN = of(value -> ByteTag.valueOf(value ? (byte) 1 : (byte) 0), tag -> ((NumericTag) tag).getAsByte() != 0);
    AttachmentSerializer<String> STRING = of(StringTag::valueOf, Tag::getAsString);
    AttachmentSerializer<CompoundTag> COMPOUND = of(CompoundTag::copy, tag -> ((CompoundTag) tag).copy());
    AttachmentSerializer<UUID> UUID_VALUE = of(NbtUtils::createUUID, NbtUtils::loadUUID);
    AttachmentSerializer<BlockPos> BLOCK_POS = of(NbtUtils::writeBlockPos, tag -> {
        final int[] values = ((IntArrayTag) tag).getAsIntArray();
        return new BlockPos(values[0], values[1], values[2]);
    });

    /**
     * Helper to build a serializer for method references
     */
    static <T> AttachmentSerializer<T> of(Function<T, Tag> writer, Function<Tag, T> reader) {
        return new AttachmentSerializer<>() {

            @Override
            public Tag write(T value) {
                return writer.apply(value);
            }

            @Override
            public T read(Tag tag) {
                return reader.apply(tag);
            }

        };

    }

}
