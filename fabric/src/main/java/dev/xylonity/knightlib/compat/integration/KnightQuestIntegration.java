package dev.xylonity.knightlib.compat.integration;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import dev.xylonity.knightquest.common.entity.boss.NethermanEntity;
import dev.xylonity.knightquest.config.values.KQConfigValues;
import dev.xylonity.knightquest.registry.KnightQuestEntities;
import dev.xylonity.knightquest.registry.KnightQuestItems;

/**
 * Bridge to avoid crashes when specific mods are not present and some internal classes is called.
 */
public class KnightQuestIntegration {

    public static Item getRadiantEssence() {
        return KnightQuestItems.RADIANT_ESSENCE.get();
    }

    public static Item getEmptyGoblet() {
        return KnightQuestItems.EMPTY_GOBLET.get();
    }

    public static Item getFilledGoblet() {
        return KnightQuestItems.FILLED_GOBLET.get();
    }

    public static boolean configCanSummonNetherman() {
        return KQConfigValues.CAN_SUMMON_NETHERMAN;
    }

    public static boolean configSpawnLightningOnSpawn() {
        return KQConfigValues.SPAWN_LIGHTNING_ON_SPAWN;
    }

    public static EntityType<NethermanEntity> nethermanEntity() {
        return KnightQuestEntities.NETHERMAN;
    }

}
