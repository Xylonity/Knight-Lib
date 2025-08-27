package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.entity.projectile.GreatChaliceStartsetRing;
import dev.xylonity.knightlib.api.registrar.ResourceDispatcher;
import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.api.registrar.ResourceType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.List;

public class KnightLibEntities {

    public static final ResourceRegistry<EntityType<?>> ENTITIES = ResourceDispatcher.create(ResourceType.ENTITIES, KnightLib.MOD_ID);

    public static final ResourceEntry<EntityType<GreatChaliceStartsetRing>> GREAT_CHALICE_STARSET_RING;

    static {
        GREAT_CHALICE_STARSET_RING = ENTITIES.registerEntity("great_chalice_starset_ring", GreatChaliceStartsetRing::new, MobCategory.MISC, 0.1f, 0.1f, List.of(EntityType.Builder::noSummon, b -> b.clientTrackingRange(128), b -> b.updateInterval(1)));
    }

}
