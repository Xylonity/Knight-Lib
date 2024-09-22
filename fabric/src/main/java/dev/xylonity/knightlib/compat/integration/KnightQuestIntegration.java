package dev.xylonity.knightlib.compat.integration;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.xylonity.common.entity.boss.NethermanEntity;
import net.xylonity.config.values.KQConfigValues;
import net.xylonity.registry.KnightQuestEntities;
import net.xylonity.registry.KnightQuestItems;

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
