package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fired when a loot table is being loaded, allowing modification via pool injection
 */
public final class LootTableModifyEvent extends KnightLibEvent {

    private final ResourceLocation id;
    private final List<LootPool.Builder> pendingPools = new ArrayList<>();

    public LootTableModifyEvent(ResourceLocation id) {
        this.id = id;
    }

    /**
     * The resource location of the loot table being loaded.
     * For example {@code minecraft:entities/zombie} or {@code minecraft:chests/stronghold}
     */
    public ResourceLocation getId() {
        return id;
    }

    /**
     * Adds a loot pool to this table
     */
    public void addPool(LootPool.Builder pool) {
        pendingPools.add(pool);
    }

    /**
     * Returns all pools. Internal
     */
    public List<LootPool.Builder> getPendingPools() {
        return Collections.unmodifiableList(pendingPools);
    }

    public boolean isEntityTable() {
        return id.getPath().startsWith("entities/");
    }

    public boolean isChestTable() {
        return id.getPath().startsWith("chests/");
    }

}