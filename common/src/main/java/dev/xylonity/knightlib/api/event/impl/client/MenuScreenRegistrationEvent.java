package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Event to register screens and containers
 */
public abstract class MenuScreenRegistrationEvent extends KnightLibEvent {

    @Override
    public boolean isSticky() {
        return true;
    }

    @FunctionalInterface
    public interface ScreenFactory<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> {
        S create(M menu, Inventory playerInventory, Component title);
    }

    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(ResourceEntry<MenuType<M>> menuEntry, ScreenFactory<M, S> screenFactory) {
        register(menuEntry.get(), screenFactory);
    }

    public abstract <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(MenuType<M> menuType, ScreenFactory<M, S> screenFactory);

}