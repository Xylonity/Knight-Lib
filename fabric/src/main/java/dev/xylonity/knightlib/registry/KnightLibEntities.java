package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.common.entity.projectile.GreatChaliceStartsetRing;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class KnightLibEntities {

    public static void init() { ;; }

    public static final EntityType<GreatChaliceStartsetRing> GREAT_CHALICE_STARSET_RING;

    static {
        GREAT_CHALICE_STARSET_RING = register("great_chalice_starset_ring", GreatChaliceStartsetRing::new, MobCategory.MISC, 0.1f, 0.1f, List.of(EntityType.Builder::noSummon, b -> b.clientTrackingRange(128), b -> b.updateInterval(1)));
    }

    private static <X extends Entity> EntityType<X> register(String name, EntityType.EntityFactory<X> entity, MobCategory category, float width, float height, @Nullable List<Consumer<EntityType.Builder<X>>> properties) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(KnightLib.MOD_ID, name), buildEntity(name, entity, category, width, height, properties));
    }

    private static <X extends Entity> EntityType<X> buildEntity(String name, EntityType.EntityFactory<X> entity, MobCategory category, float width, float height, @Nullable List<Consumer<EntityType.Builder<X>>> properties) {
        EntityType.Builder<X> builder = EntityType.Builder.of(entity, category).sized(width, height);

        if (properties != null) {
            for (Consumer<EntityType.Builder<X>> property : properties) {
                property.accept(builder);
            }
        }

        return builder.build(ResourceLocation.fromNamespaceAndPath(KnightLib.MOD_ID, name).toString());
    }

}
