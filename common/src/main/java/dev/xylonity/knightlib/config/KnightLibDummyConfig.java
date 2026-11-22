package dev.xylonity.knightlib.config;

import dev.xylonity.knightlib.api.config.AutoConfig;
import dev.xylonity.knightlib.api.config.ConfigEntry;
import dev.xylonity.knightlib.api.config.ConfigRange;
import dev.xylonity.knightlib.api.config.DecorationType;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference config with one of every kind of entry the config screen supports, to copy from in other mods. It is not
 * registered, so it has no file and doesn't show in the config screen. To try it, register it from the loader entry
 * points with {@code ConfigComposer.registerConfig(KnightLib.MOD_ID, KnightLibDummyConfig.class)}
 */
@AutoConfig(
        file = "knightlib_dummy",
        title = "Dummy Config",
        description = "Every kind of entry the config screen supports",
        accentColor = 0xFF8E7CFF,
        style = DecorationType.VERBOSE,
        categoryBanner = true,
        order = 1
)
public final class KnightLibDummyConfig {

    // Toggles

    @ConfigEntry(category = "Toggles", comment = "A toggle that starts on")
    public static boolean TOGGLE_ON = true;

    @ConfigEntry(category = "Toggles", comment = "A toggle that starts off")
    public static boolean TOGGLE_OFF = false;

    @ConfigEntry(category = "Toggles", comment = "Shows the restart badge next to its name", requiresRestart = true)
    public static boolean TOGGLE_NEEDS_RESTART = false;

    @ConfigEntry(category = "Toggles", comment = "Rest the cursor on the name to read the note", note = "This is the note, it only shows in the tooltip and in the TOML comment")
    public static boolean TOGGLE_WITH_NOTE = true;

    // Sliders

    @ConfigEntry(category = "Sliders", comment = "Integer slider with few steps, it gets a notch per value", min = 0, max = 5, slider = true)
    public static int SLIDER_FEW_STEPS = 2;

    @ConfigEntry(category = "Sliders", comment = "Integer slider with a wide range", min = 0, max = 1000, slider = true)
    public static int SLIDER_WIDE = 250;

    @ConfigEntry(category = "Sliders", comment = "Decimal slider between 0 and 1, starting low", min = 0, max = 1, slider = true)
    public static double SLIDER_FRACTION = 0.05d;

    @ConfigEntry(category = "Sliders", comment = "Float slider that goes negative", min = -10, max = 10, slider = true)
    public static float SLIDER_NEGATIVE = -2.5f;

    @ConfigEntry(category = "Sliders", comment = "Long slider with big numbers", min = 0, max = 100000, slider = true, requiresRestart = true)
    public static long SLIDER_LONG = 20000L;

    // Numbers

    @ConfigEntry(category = "Numbers", comment = "Typed integer between 1 and 64, anything else turns the field red", min = 1, max = 64)
    public static int NUMBER_BOUNDED = 16;

    @ConfigEntry(category = "Numbers", comment = "Typed integer without bounds, also takes hex like #FF8800")
    public static int NUMBER_UNBOUNDED = -42;

    @ConfigEntry(category = "Numbers", comment = "A long")
    public static long NUMBER_LONG = 9_000_000_000L;

    @ConfigEntry(category = "Numbers", comment = "A float with decimals", min = 0, max = 100)
    public static float NUMBER_FLOAT = 12.5f;

    @ConfigEntry(category = "Numbers", comment = "A double with a small value")
    public static double NUMBER_PRECISE = 0.0001d;

    @ConfigEntry(category = "Numbers")
    public static int NUMBER_WITHOUT_COMMENT = 3;

    // Text

    @ConfigEntry(category = "Text", comment = "Empty by default, so it shows the hint")
    public static String TEXT_EMPTY = "";

    @ConfigEntry(category = "Text", comment = "A short text")
    public static String TEXT_SHORT = "Hello";

    @ConfigEntry(category = "Text", comment = "A long text, the field starts wider")
    public static String TEXT_LONG = "The cake is a lie";

    @ConfigEntry(
            category = "Text",
            comment = "The cake is a lie The cake is a lie The cake is a lie The cake is a lie The cake is a lie The cake is a lie, "
                    + "The cake is a lie The cake is a lie The cake is a lie The cake is a lie is it The cake is a lie The cake is a lie"
    )
    public static String TEXT_LONG_DESCRIPTION = "wrapped";

    // Choices

    @ConfigEntry(category = "Choices", comment = "Pick one of a few fixed options", options = {"Peaceful", "Easy", "Normal", "Hard", "Nightmare"})
    public static String CHOICE_DIFFICULTY = "Normal";

    @ConfigEntry(category = "Choices", comment = "Only two options, no search field", options = {"Left", "Right"})
    public static String CHOICE_SIDE = "Left";

    @ConfigEntry(category = "Choices", comment = "Pick one item from the item registry", registry = "minecraft:item")
    public static String CHOICE_ITEM = "minecraft:diamond_sword";

    @ConfigEntry(category = "Choices", comment = "Pick one sound, ids get long here", registry = "minecraft:sound_event")
    public static String CHOICE_SOUND = "minecraft:entity.experience_orb.pickup";

    @ConfigEntry(category = "Choices", comment = "Small enum, cycles on click (right click goes back)")
    public static Mode CHOICE_SMALL_ENUM = Mode.BALANCED;

    @ConfigEntry(category = "Choices", comment = "Enum with many values, gets a dropdown")
    public static Dye CHOICE_BIG_ENUM = Dye.LIGHT_BLUE;

    // Lists

    @ConfigEntry(category = "Lists", comment = "Free list of texts, edited one value per line")
    public static List<String> LIST_TEXT = new ArrayList<>(List.of("first value", "second value"));

    @ConfigEntry(category = "Lists", comment = "Free list that starts empty")
    public static List<String> LIST_EMPTY = new ArrayList<>();

    @ConfigEntry(category = "Lists", comment = "Up to three values", maxSize = 3)
    public static List<String> LIST_LIMITED = new ArrayList<>(List.of("one"));

    @ConfigEntry(category = "Lists", comment = "At least two values, the remove buttons stop working at two", minSize = 2)
    public static List<String> LIST_AT_LEAST_TWO = new ArrayList<>(List.of("first", "second"));

    @ConfigEntry(category = "Lists", comment = "Integers between 0 and 100, the rest turn red and are left out", min = 0, max = 100)
    public static List<Integer> LIST_INTEGERS = new ArrayList<>(List.of(10, 20, 30));

    @ConfigEntry(category = "Lists", comment = "Decimals")
    public static List<Double> LIST_DECIMALS = new ArrayList<>(List.of(0.5d, 1.5d));

    @ConfigEntry(category = "Lists", comment = "Pick any of a few options", options = {"Overworld", "Nether", "End", "Deep Dark", "Ocean"})
    public static List<String> LIST_OPTIONS = new ArrayList<>(List.of("Overworld"));

    @ConfigEntry(category = "Lists", comment = "Pick at most two", options = {"Sword", "Axe", "Bow", "Crossbow", "Trident", "Mace"}, maxSize = 2)
    public static List<String> LIST_OPTIONS_LIMITED = new ArrayList<>(List.of("Sword"));

    @ConfigEntry(category = "Lists", comment = "Pick between one and three, the last one can't be unpicked", options = {"Spring", "Summer", "Autumn", "Winter"}, minSize = 1, maxSize = 3)
    public static List<String> LIST_OPTIONS_BETWEEN = new ArrayList<>(List.of("Summer"));

    @ConfigEntry(category = "Lists", comment = "Numbers picked from fixed options", options = {"1", "2", "4", "8", "16", "32"})
    public static List<Integer> LIST_NUMBER_OPTIONS = new ArrayList<>(List.of(4));

    @ConfigEntry(category = "Lists", comment = "Pick any number of elements, from an enum")
    public static List<Element> LIST_ENUM = new ArrayList<>(List.of(Element.FIRE, Element.ICE));

    @ConfigEntry(category = "Lists", comment = "Mobs from the entity registry, with search", registry = "minecraft:entity_type")
    public static List<String> LIST_ENTITIES = new ArrayList<>(List.of("minecraft:zombie", "minecraft:skeleton"));

    @ConfigEntry(category = "Lists", comment = "Mobs or mob tags, tags are listed inside a world and typed starting with # outside one", registry = "minecraft:entity_type", tags = true)
    public static List<String> LIST_ENTITIES_AND_TAGS = new ArrayList<>(List.of("#minecraft:skeletons", "minecraft:zombie"));

    @ConfigEntry(category = "Lists", comment = "The whole item registry, over a thousand values", registry = "minecraft:item")
    public static List<String> LIST_ITEMS = new ArrayList<>();

    @ConfigEntry(category = "Lists", comment = "Biomes only exist inside a world, outside one the ids are typed instead", registry = "minecraft:worldgen/biome")
    public static List<String> LIST_BIOMES = new ArrayList<>(List.of("minecraft:plains"));

    // Records

    @ConfigEntry(category = "Records", comment = "Ints and floats in one entry, each with its own range", requiresRestart = true)
    public static ArmorValues RECORD_ARMOR = new ArmorValues(3, 8, 6, 3, 2f, 0f, 33, 20);

    @ConfigEntry(category = "Records", comment = "Decimals without bounds, negative ones too")
    public static Offset RECORD_OFFSET = new Offset(0d, 1.5d, -0.25d);

    // Odd categories

    @ConfigEntry(comment = "No category, it ends up under General")
    public static boolean UNCATEGORIZED = true;

    @ConfigEntry(category = "A Category With A Name Long Enough To Get Cut In The Sidebar", comment = "The cake is not a lie")
    public static int LONG_CATEGORY_VALUE = 7;

    public enum Mode {
        SAFE,
        BALANCED,
        RECKLESS
    }

    public enum Dye {
        WHITE, ORANGE, MAGENTA, LIGHT_BLUE, YELLOW, LIME, PINK, GRAY,
        LIGHT_GRAY, CYAN, PURPLE, BLUE, BROWN, GREEN, RED, BLACK
    }

    public enum Element {
        FIRE, WATER, EARTH, AIR, LIGHTNING, ICE, SHADOW, LIGHT
    }

    public record ArmorValues(
            @ConfigRange(min = 0, max = 30) int helmet,
            @ConfigRange(min = 0, max = 30) int chestplate,
            @ConfigRange(min = 0, max = 30) int leggings,
            @ConfigRange(min = 0, max = 30) int boots,
            @ConfigRange(min = 0, max = 20) float toughness,
            @ConfigRange(min = 0, max = 1) float knockbackResistance,
            @ConfigRange(min = 1, max = 100) int durabilityMultiplier,
            @ConfigRange(min = 0, max = 50) int enchantmentValue
    ) {}

    public record Offset(
            double x,
            double y,
            double z
    ) {
        ;;
    }

}