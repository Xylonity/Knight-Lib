package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLibCommon;
import dev.xylonity.knightlib.common.entity.projectile.GreatChaliceStartsetRing;
import dev.xylonity.knightlib.registry.registrar.ResourceDispatcher;
import dev.xylonity.knightlib.registry.registrar.ResourceEntry;
import dev.xylonity.knightlib.registry.registrar.ResourceRegistry;
import dev.xylonity.knightlib.registry.registrar.ResourceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class KnightLibEntities {

    public static final ResourceRegistry<EntityType<?>> ENTITIES = ResourceDispatcher.create(ResourceType.ENTITIES, KnightLibCommon.MOD_ID);

    public static final ResourceEntry<EntityType<GreatChaliceStartsetRing>> GREAT_CHALICE_STARSET_RING;

    static {
        GREAT_CHALICE_STARSET_RING = register("great_chalice_starset_ring", GreatChaliceStartsetRing::new, MobCategory.MISC, 0.1f, 0.1f, List.of(EntityType.Builder::noSummon, b -> b.clientTrackingRange(128), b -> b.updateInterval(1)));
    }

    private static <X extends Entity> ResourceEntry<EntityType<X>> register(String name, EntityType.EntityFactory<X> entity, MobCategory category, float width, float height, @Nullable List<Consumer<EntityType.Builder<X>>> properties) {
        return ENTITIES.register(name, () -> {
            EntityType.Builder<X> builder = EntityType.Builder.of(entity, category).sized(width, height);

            if (properties != null) {
                for (Consumer<EntityType.Builder<X>> property : properties) {
                    property.accept(builder);
                }
            }

            return builder.build(new ResourceLocation(KnightLibCommon.MOD_ID, name).toString());
        });
    }

}
