package dev.xylonity.knightlib.api.armor;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

/**
 * Armor material abstraction for fast handling the registration process (that differs between 1.20.1 and 1.21.1, which is a headache)
 *
 * <p>Usage:</p>
 * <pre>{@code
 * public static final KnightLibArmorMaterial MAGE = KnightLibArmorMaterial.builder("mage", Companions.MOD_ID)
 *         .defense(3, 8, 6, 3)
 *         .toughness(2)
 *         .knockbackResistance(0)
 *         .durabilityMultiplier(33)
 *         .enchantmentValue(20)
 *         .equipSound(SoundEvents.ARMOR_EQUIP_DIAMOND)
 *         .repairItem(Items.DIAMOND)
 *         .build();
 * }</pre>
 *
 * <p>Then to receive the actual material, use {@code MAGE.get()}. In 1.21 durability is no longer part of
 * {@link ArmorMaterial}, so apply {@link #getDurabilityForType(ArmorItem.Type)} on the {@code Item.Properties} of each piece.</p>
 */
public final class KnightLibArmorMaterial {

    private static final EnumMap<ArmorItem.Type, Integer> BASE_DURABILITY = Util.make(
            new EnumMap<>(ArmorItem.Type.class), enumMap -> {
                enumMap.put(ArmorItem.Type.HELMET, 11);
                enumMap.put(ArmorItem.Type.CHESTPLATE, 16);
                enumMap.put(ArmorItem.Type.LEGGINGS, 15);
                enumMap.put(ArmorItem.Type.BOOTS, 13);
            }

    );

    private final String name;
    private final EnumMap<ArmorItem.Type, Integer> defense;
    private final float toughness;
    private final float knockbackResistance;
    private final int durabilityMultiplier;
    private final int enchantmentValue;
    private final Holder<SoundEvent> equipSound;
    private final Supplier<Ingredient> repairIngredient;
    private final ResourceLocation layerTexture;

    private KnightLibArmorMaterial(Builder builder) {
        this.name = builder.modId + ":" + builder.name;
        this.defense = builder.defense;
        this.toughness = builder.toughness;
        this.knockbackResistance = builder.knockbackResistance;
        this.durabilityMultiplier = builder.durabilityMultiplier;
        this.enchantmentValue = builder.enchantmentValue;
        this.equipSound = builder.equipSound;
        this.repairIngredient = builder.repairIngredient;
        this.layerTexture = ResourceLocation.fromNamespaceAndPath(builder.modId, builder.name);
    }

    public ArmorMaterial get() {
        return new ArmorMaterial(
                defense,
                enchantmentValue,
                equipSound,
                repairIngredient,
                List.of(new ArmorMaterial.Layer(layerTexture)),
                toughness,
                knockbackResistance
        );

    }

    public String getName() {
        return name;
    }

    public int getDurabilityForType(ArmorItem.Type type) {
        return BASE_DURABILITY.getOrDefault(type, 0) * durabilityMultiplier;
    }

    public static Builder builder(String name, String modId) {
        return new Builder(name, modId);
    }

    public static final class Builder {

        private final String name;
        private final String modId;
        private final EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        private float toughness = 0f;
        private float knockbackResistance = 0f;
        private int durabilityMultiplier = 15;
        private int enchantmentValue = 9;
        private Holder<SoundEvent> equipSound = SoundEvents.ARMOR_EQUIP_IRON;
        private Supplier<Ingredient> repairIngredient = () -> Ingredient.EMPTY;

        private Builder(String name, String modId) {
            this.name = name;
            this.modId = modId;
        }

        /**
         * Sets per-slot defense values
         * @param helmet helmet defense
         * @param chestplate chestplate defense
         * @param leggings leggings defense
         * @param boots boots defense
         */
        public Builder defense(int helmet, int chestplate, int leggings, int boots) {
            defense.put(ArmorItem.Type.HELMET, helmet);
            defense.put(ArmorItem.Type.CHESTPLATE, chestplate);
            defense.put(ArmorItem.Type.LEGGINGS, leggings);
            defense.put(ArmorItem.Type.BOOTS, boots);
            return this;
        }

        public Builder toughness(float toughness) {
            this.toughness = toughness;
            return this;
        }

        public Builder knockbackResistance(float knockbackResistance) {
            this.knockbackResistance = knockbackResistance;
            return this;
        }

        /**
         * Multiplier applied to the base durability per slot (11/16/15/13)
         */
        public Builder durabilityMultiplier(int durabilityMultiplier) {
            this.durabilityMultiplier = durabilityMultiplier;
            return this;
        }

        public Builder enchantmentValue(int enchantmentValue) {
            this.enchantmentValue = enchantmentValue;
            return this;
        }

        public Builder equipSound(Holder<SoundEvent> equipSound) {
            this.equipSound = equipSound;
            return this;
        }

        public Builder repairItem(ItemLike item) {
            this.repairIngredient = () -> Ingredient.of(item);
            return this;
        }

        public Builder repairIngredient(Supplier<Ingredient> ingredient) {
            this.repairIngredient = ingredient;
            return this;
        }

        public KnightLibArmorMaterial build() {
            return new KnightLibArmorMaterial(this);
        }

    }

}