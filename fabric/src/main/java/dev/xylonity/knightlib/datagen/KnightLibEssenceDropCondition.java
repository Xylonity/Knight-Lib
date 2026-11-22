package dev.xylonity.knightlib.datagen;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.config.KnightLibConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Set;

public record KnightLibEssenceDropCondition(
        boolean great
) implements LootItemCondition {

    public static final LootItemConditionType TYPE = new LootItemConditionType(new Serializer<KnightLibEssenceDropCondition>() {

        @Override
        public void serialize(JsonObject json, KnightLibEssenceDropCondition condition, JsonSerializationContext context) {
            json.addProperty("great", condition.great());
        }

        @Override
        public KnightLibEssenceDropCondition deserialize(JsonObject json, JsonDeserializationContext context) {
            return new KnightLibEssenceDropCondition(GsonHelper.getAsBoolean(json, "great"));
        }

    });

    public static void register() {
        Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, KnightLib.of("essence_drop"), TYPE);
    }

    @Override
    public LootItemConditionType getType() {
        return TYPE;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.THIS_ENTITY);
    }

    @Override
    public boolean test(LootContext context) {
        final Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (entity == null || !KnightLibConfig.isEntityAllowedForEssence(entity.getType())) {
            return false;
        }

        final double chance = great ? KnightLibConfig.GREAT_ESSENCE_DROP_RATE : KnightLibConfig.SMALL_ESSENCE_DROP_RATE;
        return context.getRandom().nextFloat() < chance;
    }

}
