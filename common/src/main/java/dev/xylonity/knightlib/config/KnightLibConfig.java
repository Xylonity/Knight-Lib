package dev.xylonity.knightlib.config;

import dev.xylonity.knightlib.api.config.AutoConfig;
import dev.xylonity.knightlib.api.config.ConfigEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.List;

@AutoConfig(
        file = "knightlib",
        title = "KnightLib Common Config",
        description = "Core config shared across KnightLib mods.",
        accentColor = 0xFFF0E545
)
public final class KnightLibConfig {

    @ConfigEntry(
            category = "Essence Drop Probabilities",
            comment = "Chance to drop small essence upon defeating an enemy",
            min = 0.0d,
            max = 1.0d,
            slider = true
    )
    public static double SMALL_ESSENCE_DROP_RATE = 0.125d;

    @ConfigEntry(
            category = "Essence Drop Probabilities",
            comment = "Chance to drop great essence upon defeating an enemy",
            min = 0.0d,
            max = 1.0d,
            slider = true
    )
    public static double GREAT_ESSENCE_DROP_RATE = 0.05d;

    @ConfigEntry(
            category = "Essence Drop Probabilities",
            comment = "Entities that never drop essence",
            registry = "minecraft:entity_type",
            tags = true
    )
    public static List<String> ESSENCE_ENTITY_BLACKLIST = new ArrayList<>();

    @ConfigEntry(
            category = "Essence Drop Probabilities",
            comment = "Entities that are the only ones allowed to drop essence. Leave empty to allow all monsters (except blacklisted ones)",
            registry = "minecraft:entity_type",
            tags = true
    )
    public static List<String> ESSENCE_ENTITY_WHITELIST = new ArrayList<>();

    @ConfigEntry(
            category = "Great Chalice",
            comment = "Spawns the filled grail (or other rewards) as an item on top of the great chalice. If the option is disabled, the item will be given to the player automatically."
    )
    public static boolean YEET_GRAIL_WHEN_FILLED = true;

    public static boolean isEntityAllowedForEssence(EntityType<?> entityType) {
        final ResourceLocation id = EntityType.getKey(entityType);

        if (contains(ESSENCE_ENTITY_BLACKLIST, entityType, id)) {
            return false;
        }

        final List<String> whitelist = ESSENCE_ENTITY_WHITELIST;
        return whitelist.isEmpty() ? entityType.getCategory() == MobCategory.MONSTER : contains(whitelist, entityType, id);
    }

    /**
     * Ids typed without a namespace count as vanilla ones (the same way the game reads them)
     */
    private static boolean contains(List<String> ids, EntityType<?> entityType, ResourceLocation id) {
        for (final String entry : ids) {
            final String trimmed = entry.trim();
            if (trimmed.startsWith("#")) {
                final ResourceLocation tag = ResourceLocation.tryParse(trimmed.substring(1));
                if (tag != null && entityType.is(TagKey.create(Registries.ENTITY_TYPE, tag))) {
                    return true;
                }

            }
            else if (id.equals(ResourceLocation.tryParse(trimmed))) {
                return true;
            }

        }

        return false;
    }

}
