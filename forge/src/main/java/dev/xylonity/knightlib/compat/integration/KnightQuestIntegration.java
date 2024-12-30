package dev.xylonity.knightlib.compat.integration;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.xylonity.knightquest.common.entity.boss.NethermanEntity;
import net.xylonity.knightquest.registry.KnightQuestEntities;
import net.xylonity.knightquest.registry.KnightQuestItems;

/**
 * Bridge to avoid crashes when specific mods are not present and some internal classes is called.
 */
public class KnightQuestIntegration {

    public static Item getRadiantEssence() {
        return KnightQuestItems.RADIANT_ESSENCE.get();
    }

    public static Item getChaoticEssence() {
        return KnightQuestItems.CHAOTIC_ESSENCE.get();
    }

    public static Item getEmptyGoblet() {
        return KnightQuestItems.EMPTY_GOBLET.get();
    }

    public static Item getFilledGoblet() {
        return KnightQuestItems.FILLED_GOBLET.get();
    }

    public static EntityType<NethermanEntity> nethermanEntity() {
        return KnightQuestEntities.NETHERMAN.get();
    }

}
