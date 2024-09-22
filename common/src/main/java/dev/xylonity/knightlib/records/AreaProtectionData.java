package dev.xylonity.knightlib.records;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public record AreaProtectionData(
        String regionName,
        BlockPos corner1,
        BlockPos corner2,
        List<ServerPlayer> allowedPlayers,
        boolean isProtected
) {

    /**
     * Checks if the player is allowed to modify blocks in the protected area.
     *
     * @param player The player to check.
     * @return True if the player is allowed, false otherwise.
     */
    public boolean isPlayerAllowed(ServerPlayer player) {
        return allowedPlayers.contains(player);
    }

    /**
     * Determines if a given position is within the protected area.
     *
     * @param position The position to check.
     * @return True if the position is within the protected area, false otherwise.
     */
    public boolean isPositionInProtectedArea(BlockPos position) {
        return position.getX() >= Math.min(corner1.getX(), corner2.getX()) &&
                position.getX() <= Math.max(corner1.getX(), corner2.getX()) &&
                position.getY() >= Math.min(corner1.getY(), corner2.getY()) &&
                position.getY() <= Math.max(corner1.getY(), corner2.getY()) &&
                position.getZ() >= Math.min(corner1.getZ(), corner2.getZ()) &&
                position.getZ() <= Math.max(corner1.getZ(), corner2.getZ());
    }

    /**
     * Adds a player to the list of allowed players for this protected area.
     *
     * @param player The player to allow.
     */
    public void allowPlayer(ServerPlayer player) {
        allowedPlayers.add(player);
    }

    @Override
    public String toString() {
        return "Protected area [" + regionName + "] from " + corner1 + " to " + corner2 + " [Players allowed: " + allowedPlayers.size() + "]";
    }

}