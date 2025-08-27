package dev.xylonity.knightlib.mixin;

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
    private static final String knightlib$GREAT_CHALICE = new ResourceLocation(KnightLib.MOD_ID, "great_chalice").toString();
    @Unique
    private static final String knightlib$HOMUNCULUS = new ResourceLocation(KnightLib.MOD_ID, "homunculus").toString();
    @Unique
    private static final String knightlib$EMPTY_GRAIL = new ResourceLocation(KnightLib.MOD_ID, "empty_grail").toString();
    @Unique
    private static final String knightlib$SMALL_ESSENCE = new ResourceLocation(KnightLib.MOD_ID, "small_essence").toString();
    @Unique
    private static final String knightlib$GREAT_ESSENCE = new ResourceLocation(KnightLib.MOD_ID, "great_essence").toString();

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("HEAD"))
    private void knightlib$apply(Map<?, ?> map, ResourceManager resourceManager, ProfilerFiller profilerFiller, CallbackInfo ci) {

        if (map == null || map.isEmpty()) return;

        boolean greatChalice = !KnightLib.isEnabled(KnightLib.Usage.GREAT_CHALICE);
        boolean homunculus = !KnightLib.isEnabled(KnightLib.Usage.HOMUNCULUS);
        boolean emptyGrail = !KnightLib.isEnabled(KnightLib.Usage.COPPER_GRAILS);
        boolean essences = !KnightLib.isEnabled(KnightLib.Usage.GREEN_ESSENCES);
        if (!greatChalice && !homunculus && !emptyGrail && !essences) return;

        Object entry = null;
        for (Object v : map.values()) {
            entry = v;
            break;
        }

        if (entry == null) return;

        if (entry instanceof JsonElement) {
            Iterator<Map.Entry<ResourceLocation, JsonElement>> iterator = ((Map<ResourceLocation, JsonElement>) map).entrySet().iterator();
            while (iterator.hasNext()) {

                Map.Entry<ResourceLocation, JsonElement> element = iterator.next();
                if (!(element.getValue() instanceof JsonObject object)) {
                    continue;
                }

                boolean remove = false;

                // Output
                if (!remove) {
                    if (essences && (knightlib$jsonResultIs(object, knightlib$SMALL_ESSENCE) || knightlib$jsonResultIs(object, knightlib$GREAT_ESSENCE))) remove = true;
                    else if (greatChalice && knightlib$jsonResultIs(object, knightlib$GREAT_CHALICE)) remove = true;
                    else if (homunculus && knightlib$jsonResultIs(object, knightlib$HOMUNCULUS)) remove = true;
                    else if (emptyGrail && knightlib$jsonResultIs(object, knightlib$EMPTY_GRAIL)) remove = true;
                }

                // Input
                if (!remove) {
                    if (essences && (knightlib$jsonHasIngredient(object, knightlib$SMALL_ESSENCE) || knightlib$jsonHasIngredient(object, knightlib$GREAT_ESSENCE))) remove = true;
                    else if (greatChalice && knightlib$jsonHasIngredient(object, knightlib$GREAT_CHALICE)) remove = true;
                    else if (homunculus && knightlib$jsonHasIngredient(object, knightlib$HOMUNCULUS)) remove = true;
                    else if (emptyGrail && knightlib$jsonHasIngredient(object, knightlib$EMPTY_GRAIL)) remove = true;
                }

                if (remove) {
                    iterator.remove();
                }
            }

            return;
        }

        if (entry instanceof Map) {
            Collection<Map<ResourceLocation, ?>> byType = (Collection<Map<ResourceLocation, ?>>) map.values();
            for (Map<ResourceLocation, ?> byId : byType) {
                if (greatChalice) byId.remove(new ResourceLocation(KnightLib.MOD_ID, "great_chalice"));
                if (homunculus) byId.remove(new ResourceLocation(KnightLib.MOD_ID, "homunculus"));
                if (emptyGrail) byId.remove(new ResourceLocation(KnightLib.MOD_ID, "empty_grail"));
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
    private static boolean knightlib$jsonHasIngredient(JsonObject jo, String target) {
        // shapeless
        if (knightlib$arrayHasItem(jo.get("ingredients"), target)) {
            return true;
        }

        // smelting
        if (knightlib$hasItem(jo.get("ingredient"), target)) {
            return true;
        }

        // shaped
        if (knightlib$objectHasItem(jo.getAsJsonObject("key"), target)) {
            return true;
        }

        // smithing
        if (knightlib$hasItem(jo.get("base"), target)) {
            return true;
        }

        if (knightlib$hasItem(jo.get("addition"), target)) {
            return true;
        }

        if (knightlib$hasItem(jo.get("template"), target)) {
            return true;
        }

        // etc
        if (knightlib$hasItem(jo.get("input"), target)) {
            return true;
        }

        if (knightlib$arrayHasItem(jo.get("inputs"), target)) {
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
            if (target.equals(knightlib$first(object, "item", "id"))) {
                return true;
            }

            JsonElement items = object.get("items");
            if (items != null && items.isJsonArray()) {
                for (JsonElement elem : items.getAsJsonArray()) {
                    if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
                        if (target.equals(elem.getAsString())) {
                            return true;
                        }
                    }
                    else if (elem.isJsonObject()) {
                        if (target.equals(knightlib$first(elem.getAsJsonObject(), "item", "id"))) {
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

}
