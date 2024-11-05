package dev.xylonity.knightlib.compat.integration;

import dev.xylonity.knightquest.common.entity.boss.NethermanEntity;
import dev.xylonity.knightquest.config.values.KQConfigValues;
import dev.xylonity.knightquest.registry.KnightQuestEntities;
import dev.xylonity.knightquest.registry.KnightQuestItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * Bridge to avoid crashes when specific mods are not present and some internal classes is called.
 */
public class KnightQuestIntegration {

    public static Item getRadiantEssence() {
        return KnightQuestItems.RADIANT_ESSENCE;
    }

    public static Item getEmptyGoblet() {
        return KnightQuestItems.EMPTY_GOBLET;
    }

    public static Item getFilledGoblet() {
        return KnightQuestItems.FILLED_GOBLET;
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
