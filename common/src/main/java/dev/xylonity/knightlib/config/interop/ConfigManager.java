package dev.xylonity.knightlib.config.interop;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.config.api.AutoConfig;
import dev.xylonity.knightlib.config.api.ConfigEntry;
import dev.xylonity.knightlib.config.api.DecorationType;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core config manager that handles loading, saving, and clamping of annotation-driven TOML configuration files.
 */
public final class ConfigManager {

    private static Path CONFIG_DIR = Path.of("config");

    private static final Set<Class<?>> REGISTERED = Collections.synchronizedSet(new LinkedHashSet<>());

    // Per file lock to avoid unexpected crashes while concurrently trying to read and write into the same file
    private static final ConcurrentHashMap<String, Object> FILE_LOCKS = new ConcurrentHashMap<>();

    // Comment pattern to detect user edits
    private static final Pattern DEFAULT_PATTERN = Pattern.compile("Default:\\s*([^|\\n]+)");

    /**
     * Stores the code-declared default values for every config field, captured before the TOML file
     * overwrites them, mainly for the "Reset All" button (inside the config screen).
     */
    private static final ConcurrentHashMap<Field, Object> DEFAULT_VALUES = new ConcurrentHashMap<>();

    public static void init(Path configDir, Class<?>... configs) {
        CONFIG_DIR = configDir;
        for (Class<?> clazz : configs) {
            loadOrCreate(clazz);
        }

    }

    /**
     * Persists the current in-memory values of every {@link ConfigEntry} field in {@code clazz}
     * back to its TOML file on disk. Called by the config screen on "Done".
     */
    public static void save(Class<?> clazz) {
        AutoConfig meta = clazz.getAnnotation(AutoConfig.class);
        if (meta == null) {
            return;
        }

        final String fileName = meta.file() + ".toml";
        final Path tomlPath = CONFIG_DIR.resolve(fileName);
        Object lock = FILE_LOCKS.computeIfAbsent(fileName, string -> new Object());

        synchronized (lock) {
            try (CommentedFileConfig fileConfig = buildConfig(tomlPath)) {
                safeLoad(fileConfig, tomlPath);
                writeFields(clazz, fileConfig, meta);

                fileConfig.save();
            }
            catch (Exception exception) {
                KnightLib.LOGGER.error("[KnightLib] Failed to save {}: {}", fileName, exception.getMessage());
            }

        }

    }

    public static void reload(Class<?> clazz) {
        AutoConfig meta = clazz.getAnnotation(AutoConfig.class);
        if (meta == null) {
            return;
        }

        final String fileName = meta.file() + ".toml";
        final Path tomlPath = CONFIG_DIR.resolve(fileName);
        Object lock = FILE_LOCKS.computeIfAbsent(fileName, k -> new Object());

        synchronized (lock) {
            try (CommentedFileConfig fileConfig = buildConfig(tomlPath)) {
                safeLoad(fileConfig, tomlPath);
                readFields(clazz, fileConfig, meta);
            }
            catch (Exception exception) {
                KnightLib.LOGGER.error("[KnightLib] Failed to reload {}: {}", fileName, exception.getMessage());
            }

        }

    }

    /**
     * Returns the code-declared default for a specific field.
     */
    public static Object getCodeDefault(Field field) {
        return DEFAULT_VALUES.get(field);
    }

    private static void loadOrCreate(Class<?> clazz) {
        if (!REGISTERED.add(clazz)) {
            return;
        }

        AutoConfig meta = clazz.getAnnotation(AutoConfig.class);
        if (meta == null) {
            return;
        }

        captureCodeDefaults(clazz);

        final String fileName = meta.file() + ".toml";
        final Path tomlPath = CONFIG_DIR.resolve(fileName);
        Object lock = FILE_LOCKS.computeIfAbsent(fileName, k -> new Object());

        synchronized (lock) {
            try {
                ensureDirectory();

                try (CommentedFileConfig fileConfig = buildConfig(tomlPath)) {
                    safeLoad(fileConfig, tomlPath);

                    readFields(clazz, fileConfig, meta);
                    writeFields(clazz, fileConfig, meta);

                    fileConfig.save();
                }

            }
            catch (Exception exception) {
                KnightLib.LOGGER.error("[KnightLib] Error processing {}: {}", fileName, exception.getMessage(), exception);
            }

        }

    }

    private static void captureCodeDefaults(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            ConfigEntry entry = field.getAnnotation(ConfigEntry.class);
            if (entry == null) {
                continue;
            }

            field.setAccessible(true);

            try {
                DEFAULT_VALUES.putIfAbsent(field, field.get(null));
            }
            catch (Exception ignored) {
                ;;
            }

        }

    }

    /**
     * Builds a non-autosave (to avoid duplicate table crashes) file config.
     */
    private static CommentedFileConfig buildConfig(Path path) {
        return CommentedFileConfig.builder(path, TomlFormat.instance())
                .preserveInsertionOrder()
                .build();
    }

    /**
     * Safely loads a config file. If the file is corrupt, it is backed up with a backup file (.bak)
     */
    private static void safeLoad(CommentedFileConfig fileConfig, Path tomlPath) {
        if (!Files.exists(tomlPath)) {
            return;
        }

        try {
            fileConfig.load();
        }
        catch (ParsingException exception) {
            KnightLib.LOGGER.warn("[KnightLib] Corrupt config file {}, backing up and recreating: {}", tomlPath.getFileName(), exception.getMessage());
            backupCorrupt(tomlPath);
        }
        catch (Exception exception) {
            KnightLib.LOGGER.warn("[KnightLib] Could not load {}: {}", tomlPath.getFileName(), exception.getMessage());
            backupCorrupt(tomlPath);
        }

    }

    private static void backupCorrupt(Path tomlPath) {
        try {
            final Path backup = tomlPath.resolveSibling(tomlPath.getFileName() + ".bak");
            Files.move(tomlPath, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException ignored) {
            try {
                Files.deleteIfExists(tomlPath);
            }
            catch (IOException ignored2) {
                ;;
            }

        }

    }

    private static void readFields(Class<?> clazz, CommentedFileConfig fileConfig, AutoConfig meta) {
        for (Field field : clazz.getDeclaredFields()) {
            ConfigEntry entry = field.getAnnotation(ConfigEntry.class);
            if (entry == null) {
                continue;
            }

            field.setAccessible(true);
            final String path = tomlPath(entry, field);

            if (!fileConfig.contains(path)) {
                continue;
            }

            Object rawEntry = fileConfig.get(path);
            if (rawEntry == null) {
                continue;
            }

            try {
                Object value = coerce(rawEntry, field.getType(), entry);
                if (value != null) {
                    setPrimitive(field, value);
                }

            }
            catch (Exception exception) {
                KnightLib.LOGGER.warn("[KnightLib] Could not read field {}: {}", field.getName(), exception.getMessage());
            }

        }

    }

    /**
     * Writes all annotated fields into the config, grouped by category to avoid duplicate
     * TOML table headers.
     */
    private static void writeFields(Class<?> clazz, CommentedFileConfig fileConfig, AutoConfig meta) {
        final DecorationType style = meta.style();

        final LinkedHashMap<String, List<Field>> fieldsByCategory = new LinkedHashMap<>();

        for (Field field : clazz.getDeclaredFields()) {
            ConfigEntry configEntry = field.getAnnotation(ConfigEntry.class);
            if (configEntry == null) {
                continue;
            }

            field.setAccessible(true);

            fieldsByCategory.computeIfAbsent(configEntry.category(), string -> new ArrayList<>()).add(field);
        }

        final Set<String> writtenCategories = new HashSet<>();

        for (Map.Entry<String, List<Field>> mapEntry : fieldsByCategory.entrySet()) {
            final String category = mapEntry.getKey();

            // Category banner comment (once per category)
            if (meta.categoryBanner() && !category.isEmpty() && writtenCategories.add(category)) {
                fileConfig.setComment(category, wrapAndIndent(buildCategoryBanner(category, style)));
            }

            for (Field field : mapEntry.getValue()) {
                ConfigEntry configEntry = field.getAnnotation(ConfigEntry.class);
                String path = tomlPath(configEntry, field);

                final Object currentValue;
                try {
                    currentValue = field.get(null);
                }
                catch (Exception exception) {
                    continue;
                }

                // Detects if the user manually changed a value in the TOML
                final Object diskValue = fileConfig.get(path);
                final Object oldDefault = parseDefaultFromComment(fileConfig.getComment(path), field.getType());

                if (diskValue != null && oldDefault != null && !valuesEqual(diskValue, oldDefault)) {
                    final Object clampedValue = clamp(diskValue, configEntry, field.getType());
                    if (clampedValue != null) {
                        fileConfig.set(path, toTomlValue(clampedValue, field.getType()));
                    }

                }
                else {
                    // Using the in-memory value if there is no user edit, or if it's the first-time write
                    fileConfig.set(path, toTomlValue(currentValue, field.getType()));
                }

                // Updates the entry comment to reflect new changes (mainly for the default value, if it changed in code)
                final String entryComment = buildEntryComment(configEntry, currentValue, style);
                fileConfig.setComment(path, wrapAndIndent(entryComment));
            }

        }

    }

    private static String tomlPath(ConfigEntry entry, Field field) {
        final String category = entry.category().trim();
        final String name = field.getName();
        return category.isEmpty() ? name : category + "." + name;
    }

    private static Object toTomlValue(Object value, Class<?> type) {
        if (type.isEnum()) {
            return ((Enum<?>) value).name();
        }

        return value;
    }

    @SuppressWarnings(
            {"unchecked", "rawtypes"}
    )
    private static Object coerce(Object raw, Class<?> type, ConfigEntry entry) {
        if (raw == null) {
            return null;
        }

        try {
            if (type == boolean.class || type == Boolean.class) {
                if (raw instanceof Boolean bool) {
                    return bool;
                }

                return Boolean.parseBoolean(raw.toString());
            }

            if (type == int.class || type == Integer.class) {
                double doubleValue = toDouble(raw);
                doubleValue = clampDouble(doubleValue, entry.min(), entry.max());
                return (int) doubleValue;
            }

            if (type == long.class || type == Long.class) {
                double doubleValue = toDouble(raw);
                doubleValue = clampDouble(doubleValue, entry.min(), entry.max());
                return (long) doubleValue;
            }

            if (type == float.class || type == Float.class) {
                double doubleValue = toDouble(raw);
                doubleValue = clampDouble(doubleValue, entry.min(), entry.max());
                return (float) doubleValue;
            }

            if (type == double.class || type == Double.class) {
                double doubleValue = toDouble(raw);
                doubleValue = clampDouble(doubleValue, entry.min(), entry.max());
                return doubleValue;
            }

            if (type == String.class) {
                return raw.toString();
            }

            if (type.isEnum()) {
                final String name = raw.toString().trim();
                try {
                    return Enum.valueOf((Class<Enum>) type, name);
                }
                catch (IllegalArgumentException exception) {
                    for (Enum<?> constant : ((Class<Enum>) type).getEnumConstants()) {
                        if (constant.name().equalsIgnoreCase(name)) {
                            return constant;
                        }

                    }

                    return null;
                }

            }

        }
        catch (Exception exception) {
            return null;
        }

        return null;
    }

    private static double toDouble(Object raw) {
        if (raw instanceof Number number) {
            return number.doubleValue();
        }

        return Double.parseDouble(raw.toString().trim());
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Object clamp(Object raw, ConfigEntry configEntry, Class<?> type) {
        if (!(raw instanceof Number num)) {
            return raw;
        }

        double doubleValue = clampDouble(num.doubleValue(), configEntry.min(), configEntry.max());
        return switch (type.getName()) {
            case "int" -> (int) doubleValue;
            case "long" -> (long) doubleValue;
            case "float" -> (float) doubleValue;
            case "double" -> doubleValue;
            default -> raw;
        };

    }

    public static void setPrimitive(Field field, Object value) throws Exception {
        switch (field.getType().getName()) {
            case "int" -> field.setInt(null, ((Number) value).intValue());
            case "long" -> field.setLong(null, ((Number) value).longValue());
            case "float" -> field.setFloat(null, ((Number) value).floatValue());
            case "double" -> field.setDouble(null, ((Number) value).doubleValue());
            case "boolean" -> field.setBoolean(null, (Boolean) value);
            default -> field.set(null, value);
        }

    }

    private static Object parseDefaultFromComment(String comment, Class<?> type) {
        if (comment == null) {
            return null;
        }

        final Matcher matcher = DEFAULT_PATTERN.matcher(comment);
        if (!matcher.find()) {
            return null;
        }

        final String raw = matcher.group(1).trim();
        try {
            return switch (type.getName()) {
                case "int" -> Integer.parseInt(raw);
                case "long" -> Long.parseLong(raw);
                case "float" -> Float.parseFloat(raw);
                case "double" -> Double.parseDouble(raw);
                case "boolean" -> Boolean.parseBoolean(raw);
                default -> raw;
            };

        }
        catch (NumberFormatException exception) {
            return null;
        }

    }

    private static boolean valuesEqual(Object value1, Object value2) {
        if (value1 == null || value2 == null) {
            return false;
        }

        if (value1 instanceof Number number1 && value2 instanceof Number number2) {
            return Math.abs(number1.doubleValue() - number2.doubleValue()) < 1.0e-9;
        }

        return value1.equals(value2);
    }

    public static String buildCategoryBanner(String category, DecorationType style) {
        final String title = (category.isEmpty() ? "GENERAL" : category.toUpperCase()) + " SETTINGS";
        return switch (style) {
            case VERBOSE -> {
                final String line = "=".repeat(70);
                yield String.join("\n", line, centerText(title, 70), line);
            }
            case RUSTIC -> ">>>> [" + title + "] <<<<";
            case STARSET -> {
                final String line = "=".repeat(70);
                yield String.join("\n", line, centerText("«✦»  " + title + "  «✦»", 70), line);
            }
            default -> title.toLowerCase().replace(" settings", "") + " §§";
        };

    }

    public static String buildEntryComment(ConfigEntry entry, Object defaultValue, DecorationType style) {
        final String base = entry.comment().trim();
        final String note = entry.note().trim();
        final boolean needsRestart = entry.requiresRestart();

        final boolean isDecimal = defaultValue instanceof Double || defaultValue instanceof Float;
        final String defaultString = isDecimal ? formatDecimal(((Number) defaultValue).doubleValue()) : String.valueOf(defaultValue);
        final String minString = formatNum(entry.min(), isDecimal);
        final String maxString = formatNum(entry.max(), isDecimal);

        final boolean showRange = !(defaultValue instanceof Boolean) && !(defaultValue instanceof Enum) && !(defaultValue instanceof String);

        return switch (style) {
            case VERBOSE -> {
                String border = "-".repeat(64);
                StringBuilder stringBuilder = new StringBuilder(border).append("\n").append(base).append("\n");
                if (!note.isEmpty()) {
                    stringBuilder.append("\nNote: ").append(note).append("\n");
                }

                stringBuilder.append("\n- Default: ").append(defaultString);

                if (showRange) {
                    stringBuilder.append("\n- Range: ").append(minString).append(" ~ ").append(maxString);
                }
                if (needsRestart) {
                    stringBuilder.append("\n- [!] Requires game restart to take effect.");
                }

                stringBuilder.append("\n").append(border);

                yield stringBuilder.toString();
            }
            case RUSTIC -> {
                StringBuilder stringBuilder = new StringBuilder();

                stringBuilder.append("$> ").append(base).append("\n$> Default: ").append(defaultString);

                if (showRange) {
                    stringBuilder.append(" | Min: ").append(minString).append(" ~ Max: ").append(maxString);
                }

                if (!note.isEmpty()) {
                    stringBuilder.append("\n$> Note: ").append(note);
                }

                if (needsRestart) {
                    stringBuilder.append("\n$> [!] Requires game restart to take effect.");
                }

                yield stringBuilder.toString();
            }
            case STARSET -> {
                StringBuilder stringBuilder = new StringBuilder();

                stringBuilder.append("✦ ").append(base).append("\n✦ Default: ").append(defaultString);

                if (showRange) {
                    stringBuilder.append(" | Range: ").append(minString).append(" -~- ").append(maxString);
                }

                if (!note.isEmpty()) {
                    stringBuilder.append("\n✦ Note: ").append(note);
                }

                if (needsRestart) {
                    stringBuilder.append("\n✦ [!] Requires game restart to take effect.");
                }

                yield stringBuilder.toString();
            }
            default -> {
                StringBuilder stringBuilder = new StringBuilder(base).append("\n\nDefault: ").append(defaultString);

                if (showRange) {
                    stringBuilder.append("\nRange: ").append(minString).append(" ~ ").append(maxString);
                }
                if (!note.isEmpty()) {
                    stringBuilder.append("\n\nNote: ").append(note);
                }
                if (needsRestart) {
                    stringBuilder.append("\n\n[!] Requires game restart to take effect.");
                }

                yield stringBuilder.toString();
            }

        };

    }

    private static String formatDecimal(double doubleValue) {
        if (Double.isInfinite(doubleValue) || Double.isNaN(doubleValue)) {
            return Double.toString(doubleValue);
        }

        final long asLong = (long) doubleValue;
        return (doubleValue == asLong) ? asLong + ".0" : Double.toString(doubleValue);
    }

    private static String formatNum(double doubleValue, boolean forceDecimal) {
        if (Double.isInfinite(doubleValue) || Double.isNaN(doubleValue)) {
            return Double.toString(doubleValue);
        }

        final long asLong = (long) doubleValue;
        if (doubleValue == asLong) {
            return forceDecimal ? asLong + ".0" : Long.toString(asLong);
        }

        return Double.toString(doubleValue);
    }

    private static String wrapAndIndent(String comment) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String paragraph : comment.split("\n")) {
            final String[] words = paragraph.split(" ");
            int column = 0;
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                if (column + word.length() > 120 && column > 0) {
                    stringBuilder.append(" ").append(line).append("\n");
                    line = new StringBuilder();
                    column = 0;
                }
                else if (column > 0) {
                    line.append(" ");
                    column++;
                }

                line.append(word);
                column += word.length();
            }

            stringBuilder.append(" ").append(line).append("\n");
        }

        if (!stringBuilder.isEmpty() && stringBuilder.charAt(stringBuilder.length() - 1) == '\n') {
            stringBuilder.setLength(stringBuilder.length() - 1);
        }

        return stringBuilder.toString();
    }

    private static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }

    private static void ensureDirectory() throws IOException {
        if (!Files.exists(CONFIG_DIR)) {
            Files.createDirectories(CONFIG_DIR);
        }

    }

}