package dev.xylonity.knightlib.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.xylonity.knightlib.api.loot.EntityLootEntry;
import dev.xylonity.knightlib.api.loot.KnightLibLoot;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(SimpleJsonResourceReloadListener.class)
public abstract class LootTableResourcesMixin {

    @Inject(method = "scanDirectory", at = @At("RETURN"))
    private static void knightlib$injectEntityDrops(ResourceManager resources, String directory, Gson gson, Map<ResourceLocation, JsonElement> elements, CallbackInfo ci) {
        if (directory.equals(Registries.elementsDirPath(Registries.LOOT_TABLE))) {
            final List<EntityLootEntry> entries = KnightLibLoot.getEntityEntries();
            if (entries.isEmpty()) {
                return;
            }

            final TagLoader<EntityType<?>> loader = new TagLoader<>(BuiltInRegistries.ENTITY_TYPE::getOptional, Registries.tagsDirPath(Registries.ENTITY_TYPE));
            final Map<ResourceLocation, Collection<EntityType<?>>> tags = loader.loadAndBuild(resources);
            final RegistryOps<JsonElement> ops = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).createSerializationContext(JsonOps.INSTANCE);

            for (int index = 0; index < entries.size(); index++) {
                final EntityLootEntry entry = entries.get(index);
                final Set<ResourceLocation> targets = new HashSet<>();
                for (final EntityType<?> type : tags.getOrDefault(entry.getEntityTag().location(), List.of())) {
                    targets.add(type.getDefaultLootTable().location());
                }

                targets.remove(BuiltInLootTables.EMPTY.location());

                JsonObject pool = null;
                for (final ResourceLocation target : targets) {
                    final JsonElement element = elements.get(target);
                    if (element == null || !element.isJsonObject()) {
                        continue;
                    }

                    final JsonObject table = element.getAsJsonObject();
                    if (table.has("pools") && !table.get("pools").isJsonArray()) {
                        // Malformed mainly
                        continue;
                    }

                    if (pool == null) {
                        pool = LootPool.CODEC.encodeStart(ops, KnightLibLoot.buildPool(entry).build()).getOrThrow().getAsJsonObject();
                        pool.addProperty("name", "knightlib:entity_drop_" + index);
                    }

                    if (!table.has("pools")) {
                        table.add("pools", new JsonArray());
                    }

                    table.getAsJsonArray("pools").add(pool.deepCopy());
                }

            }

        }

    }

}
