package dev.xylonity.knightlib.config.interop;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.config.AutoConfig;
import dev.xylonity.knightlib.api.config.ConfigEntry;
import dev.xylonity.knightlib.api.config.ConfigRange;
import dev.xylonity.knightlib.api.config.DecorationType;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
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

                // The in-memory values are what the player just set, so they win over whatever the file holds
                writeFields(clazz, fileConfig, meta, true);

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
                    writeFields(clazz, fileConfig, meta, false);

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
                // Lists are copied, the field may later point at a list that gets edited
                final Object value = field.get(null);
                DEFAULT_VALUES.putIfAbsent(field, value instanceof List<?> list ? Collections.unmodifiableList(new ArrayList<>(list)) : value);
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
                Object value = coerceField(rawEntry, field, entry);
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
     * Writes all annotated fields into the config, grouped by category to avoid duplicate TOML table headers
     */
    private static void writeFields(Class<?> clazz, CommentedFileConfig fileConfig, AutoConfig meta, boolean fromMemory) {
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

                // Lists always go through the in-memory value, it's the file's one already parsed (an old comma string becomes an array)
                if (!fromMemory && !(currentValue instanceof List) && !(currentValue instanceof Record) && diskValue != null && oldDefault != null && !valuesEqual(diskValue, oldDefault)) {
                    final Object clampedValue = clamp(diskValue, configEntry, field.getType());
                    if (clampedValue != null) {
                        fileConfig.set(path, toTomlValue(clampedValue, field.getType()));
                    }

                }
                else {
                    // Using the in-memory value if there is no user edit, or if it's the first-time write
                    fileConfig.set(path, currentValue instanceof Record record ? recordTable(fileConfig, record) : toTomlValue(currentValue, field.getType()));
                }

                // Updates the entry comment to reflect new changes (mainly for the default value, if it changed in code)
                final String entryComment = buildEntryComment(configEntry, DEFAULT_VALUES.getOrDefault(field, currentValue), style);
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
        if (value instanceof List<?> list) {
            final List<Object> converted = new ArrayList<>(list.size());
            for (Object element : list) {
                converted.add(element instanceof Enum<?> constant ? constant.name() : element);
            }

            return converted;
        }

        if (type.isEnum()) {
            return ((Enum<?>) value).name();
        }

        return value;
    }

    public static Class<?> listElementType(Field field) {
        if (field.getGenericType() instanceof ParameterizedType parameterized && parameterized.getActualTypeArguments().length == 1 && parameterized.getActualTypeArguments()[0] instanceof Class<?> element) {
            return element;
        }

        return String.class;
    }

    /**
     * Converts a single raw value to the entry bounds
     */
    public static Object coerceElement(Object raw, Class<?> type, ConfigEntry entry) {
        return coerce(raw, type, entry);
    }

    private static Object coerceField(Object raw, Field field, ConfigEntry entry) throws IllegalAccessException {
        if (List.class.isAssignableFrom(field.getType())) {
            final List<Object> list = coerceList(raw, listElementType(field), entry);
            return list.size() < entry.minSize() ? null : list;
        }

        if (field.getType().isRecord()) {
            return coerceRecord(raw, field.getType(), field.get(null));
        }

        return coerce(raw, field.getType(), entry);
    }

    private static Object coerceRecord(Object raw, Class<?> type, Object fallback) {
        if (!(raw instanceof UnmodifiableConfig table)) {
            return null;
        }

        final RecordComponent[] components = type.getRecordComponents();
        final List<Object> fallbackValues = fallback instanceof Record record ? recordValues(record) : null;
        final List<Object> values = new ArrayList<>(components.length);
        for (int i = 0; i < components.length; i++) {
            final Object value = table.get(components[i].getName());
            values.add(value != null || fallbackValues == null ? value : fallbackValues.get(i));
        }

        return buildRecord(type, values);
    }

    public static List<Object> recordValues(Record record) {
        final RecordComponent[] components = record.getClass().getRecordComponents();
        final List<Object> values = new ArrayList<>(components.length);
        for (RecordComponent component : components) {
            try {
                final Method accessor = component.getAccessor();
                accessor.setAccessible(true);
                values.add(accessor.invoke(record));
            }
            catch (Exception exception) {
                values.add(null);
            }

        }

        return values;
    }

    public static Object buildRecord(Class<?> type, List<Object> values) {
        try {
            final RecordComponent[] components = type.getRecordComponents();
            final Class<?>[] types = new Class<?>[components.length];
            final Object[] arguments = new Object[components.length];
            for (int i = 0; i < components.length; i++) {
                types[i] = components[i].getType();

                final ConfigRange range = components[i].getAnnotation(ConfigRange.class);
                final double value = range == null ? toDouble(values.get(i)) : clampDouble(toDouble(values.get(i)), range.min(), range.max());
                arguments[i] = switch (types[i].getName()) {
                    case "int", "java.lang.Integer" -> (int) value;
                    case "long", "java.lang.Long" -> (long) value;
                    case "float", "java.lang.Float" -> (float) value;
                    case "double", "java.lang.Double" -> value;
                    default -> throw new IllegalArgumentException("Unsupported record component type " + types[i].getName());
                };
            }

            final Constructor<?> constructor = type.getDeclaredConstructor(types);
            constructor.setAccessible(true);
            return constructor.newInstance(arguments);
        }
        catch (Exception exception) {
            return null;
        }

    }

    private static CommentedConfig recordTable(CommentedFileConfig fileConfig, Record record) {
        final CommentedConfig table = fileConfig.createSubConfig();
        final RecordComponent[] components = record.getClass().getRecordComponents();
        final List<Object> values = recordValues(record);
        for (int i = 0; i < components.length; i++) {
            final String name = components[i].getName();
            table.set(name, values.get(i));

            final ConfigRange range = components[i].getAnnotation(ConfigRange.class);
            if (range != null && (Double.isFinite(range.min()) || Double.isFinite(range.max()))) {
                final boolean decimal = values.get(i) instanceof Double || values.get(i) instanceof Float;
                table.setComment(name, wrapAndIndent("Range: " + formatNum(range.min(), decimal) + " ~ " + formatNum(range.max(), decimal)));
            }

        }

        return table;
    }

    private static String formatRecord(Record record) {
        final RecordComponent[] components = record.getClass().getRecordComponents();
        final List<Object> values = recordValues(record);
        final List<String> parts = new ArrayList<>(components.length);
        for (int i = 0; i < components.length; i++) {
            final Object value = values.get(i);
            final boolean decimal = value instanceof Double || value instanceof Float;
            parts.add(components[i].getName() + " = " + (decimal ? formatDecimal(((Number) value).doubleValue()) : value));
        }

        return String.join(", ", parts);
    }

    private static List<Object> coerceList(Object raw, Class<?> elementType, ConfigEntry entry) {
        final List<?> items;
        if (raw instanceof List<?> list) {
            items = list;
        }
        else {
            final String text = raw.toString().trim();
            items = text.isEmpty() ? List.of() : Arrays.stream(text.split(",")).map(String::trim).filter(part -> !part.isEmpty()).toList();
        }

        final List<Object> result = new ArrayList<>(items.size());
        for (Object item : items) {
            if (entry.maxSize() > 0 && result.size() >= entry.maxSize()) {
                break;
            }

            final Object value = coerce(item, elementType, entry);
            if (value != null) {
                result.add(value);
            }

        }

        return result;
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
                // A list read into what is now a plain string field keeps its values (comma separated)
                if (raw instanceof List<?> list) {
                    final List<String> parts = new ArrayList<>(list.size());
                    for (Object element : list) {
                        parts.add(String.valueOf(element));
                    }

                    return String.join(",", parts);
                }

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
            default -> field.set(null, value instanceof List<?> list ? new ArrayList<>(list) : value);
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
        final String defaultString = isDecimal ? formatDecimal(((Number) defaultValue).doubleValue()) : defaultValue instanceof Record record ? formatRecord(record) : String.valueOf(defaultValue);
        final String minString = formatNum(entry.min(), isDecimal);
        final String maxString = formatNum(entry.max(), isDecimal);

        // On lists the range applies to each element so it's only worth mentioning when there is one
        final boolean showRange = defaultValue instanceof List<?> ? Double.isFinite(entry.min()) || Double.isFinite(entry.max())
                : !(defaultValue instanceof Boolean) && !(defaultValue instanceof Enum) && !(defaultValue instanceof String) && !(defaultValue instanceof Record);

        final List<String> allowed = new ArrayList<>();
        if (entry.options().length > 0) {
            allowed.add("Options: " + String.join(", ", entry.options()));
        }
        else if (!entry.registry().isEmpty()) {
            allowed.add((entry.tags() ? "Values: ids or #tags from " : "Values: ids from ") + entry.registry());
        }
        if (entry.minSize() > 0) {
            allowed.add("Min entries: " + entry.minSize());
        }
        if (entry.maxSize() > 0) {
            allowed.add("Max entries: " + entry.maxSize());
        }

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
                for (String line : allowed) {
                    stringBuilder.append("\n- ").append(line);
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

                for (String line : allowed) {
                    stringBuilder.append("\n$> ").append(line);
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

                for (String line : allowed) {
                    stringBuilder.append("\n✦ ").append(line);
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
                for (String line : allowed) {
                    stringBuilder.append("\n").append(line);
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