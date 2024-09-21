package dev.xylonity.knightlib.knightquest.compat;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.xylonity.knightquest.common.entity.boss.NethermanEntity;
import net.xylonity.knightquest.config.values.KQConfigValues;
import net.xylonity.knightquest.registry.KnightQuestEntities;
import net.xylonity.knightquest.registry.KnightQuestItems;

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
        return KnightQuestEntities.NETHERMAN.get();
    }

}
