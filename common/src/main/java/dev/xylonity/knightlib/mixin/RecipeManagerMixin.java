package dev.xylonity.knightlib.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.xylonity.knightlib.KnightLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

@Mixin(RecipeManager.class)
@SuppressWarnings("unchecked")
public class RecipeManagerMixin {

    @Unique
    private static final String knightlib$GREAT_CHALICE = KnightLib.of("great_chalice").toString();
    @Unique
    private static final String knightlib$HOMUNCULUS = KnightLib.of("homunculus").toString();
    @Unique
    private static final String knightlib$EMPTY_GRAIL = KnightLib.of("empty_grail").toString();
    @Unique
    private static final String knightlib$SMALL_ESSENCE = KnightLib.of("small_essence").toString();
    @Unique
    private static final String knightlib$GREAT_ESSENCE = KnightLib.of("great_essence").toString();

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("HEAD"))
    private void knightlib$apply(Map<?, ?> map, ResourceManager resourceManager, ProfilerFiller profilerFiller, CallbackInfo ci) {

        if (map == null || map.isEmpty()) {
            return;
        }

        boolean greatChalice = !KnightLib.isEnabled(KnightLib.Usage.GREAT_CHALICE);
        boolean homunculus = !KnightLib.isEnabled(KnightLib.Usage.HOMUNCULUS);
        boolean emptyGrail = !KnightLib.isEnabled(KnightLib.Usage.COPPER_GRAILS);
        boolean essences = !KnightLib.isEnabled(KnightLib.Usage.GREEN_ESSENCES);
        if (!greatChalice && !homunculus && !emptyGrail && !essences) {
            return;
        }

        Object entry = null;
        for (Object value : map.values()) {
            entry = value;
            break;
        }

        if (entry == null) {
            return;
        }

        // apply receives a pair RL-JsonElement
        if (entry instanceof JsonElement) {
            Iterator<Map.Entry<ResourceLocation, JsonElement>> iterator = ((Map<ResourceLocation, JsonElement>) map).entrySet().iterator();
            while (iterator.hasNext()) {

                Map.Entry<ResourceLocation, JsonElement> element = iterator.next();

                // Some recipes may not be JsonObjects
                if (!(element.getValue() instanceof JsonObject object)) {
                    continue;
                }

                boolean remove = false;

                // Output
                if (!remove) {
                    if (essences && (knightlib$jsonResultIs(object, knightlib$SMALL_ESSENCE) || knightlib$jsonResultIs(object, knightlib$GREAT_ESSENCE))) {
                        remove = true;
                    }
                    else if (greatChalice && knightlib$jsonResultIs(object, knightlib$GREAT_CHALICE)) {
                        remove = true;
                    }
                    else if (homunculus && knightlib$jsonResultIs(object, knightlib$HOMUNCULUS)) {
                        remove = true;
                    }
                    else if (emptyGrail && knightlib$jsonResultIs(object, knightlib$EMPTY_GRAIL)) {
                        remove = true;
                    }

                }

                // Input
                if (!remove) {
                    if (essences && (knightlib$jsonHasIngredient(object, knightlib$SMALL_ESSENCE) || knightlib$jsonHasIngredient(object, knightlib$GREAT_ESSENCE))) {
                        remove = true;
                    }
                    else if (greatChalice && knightlib$jsonHasIngredient(object, knightlib$GREAT_CHALICE)) {
                        remove = true;
                    }
                    else if (homunculus && knightlib$jsonHasIngredient(object, knightlib$HOMUNCULUS)) {
                        remove = true;
                    }
                    else if (emptyGrail && knightlib$jsonHasIngredient(object, knightlib$EMPTY_GRAIL)) {
                        remove = true;
                    }

                }

                if (remove) {
                    iterator.remove();
                }
            }

            return;
        }

        // After deserializing, apply receives a map without JsonObjects, thus we delete the relevant recipes
        if (entry instanceof Map) {
            Collection<Map<ResourceLocation, ?>> byType = (Collection<Map<ResourceLocation, ?>>) map.values();
            for (Map<ResourceLocation, ?> byId : byType) {
                if (greatChalice) {
                    byId.remove(KnightLib.of("great_chalice"));
                }
                if (homunculus) {
                    byId.remove(KnightLib.of("homunculus"));
                }
                if (emptyGrail) {
                    byId.remove(KnightLib.of("empty_grail"));
                }

            }

        }

    }

    @Unique
    private static boolean knightlib$jsonResultIs(JsonObject object, String target) {
        JsonElement element = object.get("result");
        if (element == null) {
            element = object.get("output");
        }

        if (element == null) {
            return false;
        }

        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return target.equals(element.getAsString());
        }

        if (element.isJsonObject()) {
            return target.equals(knightlib$first(element.getAsJsonObject(), "item", "id", "result", "output"));
        }

        if (element.isJsonArray()) {
            for (JsonElement elem : element.getAsJsonArray()) {
                if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
                    if (target.equals(elem.getAsString())) {
                        return true;
                    }

                }
                else if (elem.isJsonObject()) {
                    if (target.equals(knightlib$first(elem.getAsJsonObject(), "item", "id", "result", "output"))) {
                        return true;
                    }

                }

            }

        }

        return false;
    }

    @Unique
    private static boolean knightlib$jsonHasIngredient(JsonObject jsonObject, String target) {
        // shapeless
        if (knightlib$arrayHasItem(knightlib$getArray(jsonObject, "ingredients"), target)) {
            return true;
        }

        // smelting/blasting/smoking/etc.
        if (knightlib$hasItem(knightlib$get(jsonObject, "ingredient"), target)) {
            return true;
        }

        // shaped
        if (knightlib$objectHasItem(knightlib$getObject(jsonObject, "key"), target)) {
            return true;
        }

        // smithing
        if (knightlib$hasItem(knightlib$get(jsonObject, "base"), target)) {
            return true;
        }
        if (knightlib$hasItem(knightlib$get(jsonObject, "addition"), target)) {
            return true;
        }
        if (knightlib$hasItem(knightlib$get(jsonObject, "template"), target)) {
            return true;
        }

        // etc
        if (knightlib$hasItem(knightlib$get(jsonObject, "input"), target)) {
            return true;
        }
        if (knightlib$arrayHasItem(knightlib$getArray(jsonObject, "inputs"), target)) {
            return true;
        }

        return false;
    }

    @Unique
    private static boolean knightlib$objectHasItem(JsonObject object, String target) {
        if (object == null) {
            return false;
        }

        for (Map.Entry<String, JsonElement> element : object.entrySet()) {
            if (knightlib$hasItem(element.getValue(), target)) {
                return true;
            }

        }

        return false;
    }

    @Unique
    private static boolean knightlib$arrayHasItem(JsonElement element, String target) {
        if (element == null || !element.isJsonArray()) {
            return false;
        }

        for (JsonElement elem : element.getAsJsonArray()) {
            if (knightlib$hasItem(elem, target)) {
                return true;
            }
        }

        return false;
    }

    @Unique
    private static boolean knightlib$hasItem(JsonElement element, String target) {
        if (element == null) {
            return false;
        }

        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return target.equals(element.getAsString());
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();

            String direct = knightlib$first(object, "item", "id");
            if (target.equals(direct)) {
                return true;
            }

            JsonArray items = knightlib$getArray(object, "items");
            if (items != null) {
                for (JsonElement elem : items) {
                    if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
                        if (target.equals(elem.getAsString())) {
                            return true;
                        }

                    }
                    else if (elem.isJsonObject()) {
                        String nested = knightlib$first(elem.getAsJsonObject(), "item", "id");
                        if (target.equals(nested)) {
                            return true;
                        }
                    }
                }

            }

        }

        if (element.isJsonArray()) {
            for (JsonElement elem : element.getAsJsonArray()) {
                if (knightlib$hasItem(elem, target)) {
                    return true;
                }
            }

        }

        return false;
    }

    @Unique
    private static String knightlib$first(JsonObject object, String... keys) {
        for (String key : keys) {
            if (object.has(key)) {
                try {
                    return GsonHelper.getAsString(object, key);
                }
                catch (Exception ignored) {
                    ;;
                }
            }

        }

        return null;
    }

    @Unique
    private static JsonElement knightlib$get(JsonObject jsonObject, String key) {
        if (jsonObject == null) {
            return null;
        }

        return jsonObject.get(key);
    }

    @Unique
    private static JsonObject knightlib$getObject(JsonObject jsonObject, String key) {
        JsonElement jsonElement = knightlib$get(jsonObject, key);
        if (jsonElement != null && jsonElement.isJsonObject()) {
            return jsonElement.getAsJsonObject();
        }

        return null;
    }

    @Unique
    private static JsonArray knightlib$getArray(JsonObject jsonObject, String key) {
        JsonElement jsonElement = knightlib$get(jsonObject, key);
        if (jsonElement != null && jsonElement.isJsonArray()) {
            return jsonElement.getAsJsonArray();
        }

        return null;
    }

}