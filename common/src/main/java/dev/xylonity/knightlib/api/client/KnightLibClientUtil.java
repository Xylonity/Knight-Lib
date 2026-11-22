package dev.xylonity.knightlib.api.client;

import dev.xylonity.knightlib.mixin.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;

public final class KnightLibClientUtil {

    /**
     * Sends the selected hotbar slot to the server if it changed since the last time, so a packet sent right after is
     * checked against the item the player holds now and not the one selected before
     */
    public static void syncCarriedItem() {
        final MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode != null) {
            ((MultiPlayerGameModeAccessor) gameMode).knightlib$ensureHasSentCarriedItem();
        }

    }

}