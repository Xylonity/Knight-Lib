package dev.xylonity.knightlib.client.item;

import dev.xylonity.knightlib.api.client.item.KnightLibWeaponRenderHooks;
import dev.xylonity.knightlib.api.item.KnightLibWeaponItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Resolves and caches the render hooks of each weapon item
 */
@ApiStatus.Internal
public final class KnightLibWeaponClient {

    private static final Map<Item, Optional<KnightLibWeaponRenderHooks>> HOOKS = new ConcurrentHashMap<>();

    @Nullable
    public static KnightLibWeaponRenderHooks hooks(ItemStack stack) {
        if (!(stack.getItem() instanceof KnightLibWeaponItem weapon)) {
            return null;
        }

        return HOOKS.computeIfAbsent(stack.getItem(), item -> Optional.ofNullable(resolve(item, weapon))).orElse(null);
    }

    @Nullable
    private static KnightLibWeaponRenderHooks resolve(Item item, KnightLibWeaponItem weapon) {
        final Supplier<Object> supplier = weapon.weaponRenderHooks();
        if (supplier == null) {
            return null;
        }

        final Object hooks = supplier.get();
        if (hooks instanceof KnightLibWeaponRenderHooks weaponHooks) {
            return weaponHooks;
        }

        final String type = hooks == null ? "null" : hooks.getClass().getName();
        throw new IllegalStateException("[KnightLib] Weapon render hooks for " + BuiltInRegistries.ITEM.getKey(item) + " returned " + type + " instead of a _KnightLibWeaponRenderHooks");
    }

}
