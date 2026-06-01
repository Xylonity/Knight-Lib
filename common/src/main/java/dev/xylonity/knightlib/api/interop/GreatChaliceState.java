package dev.xylonity.knightlib.api.interop;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * Defines multiple states the chalice can permute between
 */
public enum GreatChaliceState implements StringRepresentable {
    EMPTY, NORMAL, CHAOTIC;

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase();
    }

}
