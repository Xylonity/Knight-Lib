package dev.xylonity.knightlib.api.event.impl.server;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Fired when an entity finishes using an item
 */
public class LivingUseItemFinishEvent extends KnightLibEvent {

    private final LivingEntity entity;
    private final ItemStack item;
    private ItemStack result;

    public LivingUseItemFinishEvent(LivingEntity entity, ItemStack item, ItemStack result) {
        this.entity = entity;
        this.item = item;
        this.result = result;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public ItemStack getItem() {
        return item;
    }

    public ItemStack getResult() {
        return result;
    }

    public void setResult(ItemStack result) {
        this.result = result;
    }

}