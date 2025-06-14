package dev.xylonity.knightlib.common.api;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * Defines multiple states the chalice can permute between
 */
public enum GreatChaliceState implements StringRepresentable {
    EMPTY, NORMAL, CHAOTIC, RADIANT;

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase();
    }

}
