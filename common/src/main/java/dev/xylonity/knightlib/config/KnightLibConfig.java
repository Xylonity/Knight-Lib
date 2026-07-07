package dev.xylonity.knightlib.config;

import dev.xylonity.knightlib.api.config.AutoConfig;
import dev.xylonity.knightlib.api.config.ConfigEntry;
import dev.xylonity.knightlib.api.util.KnightLibUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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
            comment = "Comma-separated list of entity IDs that should never drop essence (e.g. \"minecraft:silverfish,minecraft:endermite\")"
    )
    public static String ESSENCE_ENTITY_BLACKLIST = "";

    @ConfigEntry(
            category = "Essence Drop Probabilities",
            comment = "Comma-separated list of entity IDs that are the only ones allowed to drop essence. Leave empty to allow all monsters (except blacklisted ones)"
    )
    public static String ESSENCE_ENTITY_WHITELIST = "";

    @ConfigEntry(
            category = "Great Chalice",
            comment = "Spawns the filled grail (or other rewards) as an item on top of the great chalice. If the option is disabled, the item will be given to the player automatically."
    )
    public static boolean YEET_GRAIL_WHEN_FILLED = true;

    public static boolean isEntityAllowedForEssence(EntityType<?> entityType) {
        final ResourceLocation id = EntityType.getKey(entityType);
        final String entityId = id.toString();

        final Set<String> blacklist = KnightLibUtil.parseList(ESSENCE_ENTITY_BLACKLIST);
        if (blacklist.contains(entityId)) {
            return false;
        }

        final Set<String> whitelist = KnightLibUtil.parseList(ESSENCE_ENTITY_WHITELIST);
        if (!whitelist.isEmpty()) {
            return whitelist.contains(entityId);
        }

        return true;
    }

}
