package dev.xylonity.knightlib.client.event.impl;

import dev.xylonity.knightlib.api.event.impl.client.MenuScreenRegistrationEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public class MenuScreenRegistrationEventNeoForge extends MenuScreenRegistrationEvent {

    @Override
    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(MenuType<M> menuType, ScreenFactory<M, S> screenFactory) {
        MenuScreens.register(menuType, screenFactory::create);
    }

}