package dev.xylonity.knightlib.client.event.impl;

import dev.xylonity.knightlib.api.event.impl.client.MenuScreenRegistrationEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.ArrayList;
import java.util.List;

public class MenuScreenRegistrationEventNeoForge extends MenuScreenRegistrationEvent {

    private final List<Entry<?, ?>> entries = new ArrayList<>();

    @Override
    public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(MenuType<M> menuType, ScreenFactory<M, S> screenFactory) {
        entries.add(new Entry<>(menuType, screenFactory::create));
    }

    public void applyToForgeEvent(RegisterMenuScreensEvent event) {
        for (final Entry<?, ?> entry : entries) {
            entry.apply(event);
        }

    }

    private record Entry<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>>(
            MenuType<M> menuType,
            MenuScreens.ScreenConstructor<M, S> constructor
    ) {
        void apply(RegisterMenuScreensEvent event) {
            event.register(menuType, constructor);
        }

    }

}
