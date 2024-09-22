package dev.xylonity.knightlib.compat.registry;

import dev.xylonity.knightlib.KnightLibCommon;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * This class is meant for inner item registration for the Knight Quest series. Don't use neither the compat nor the platform packages.
 */
public class KnightLibItems {

    public static void init() { ;; }

    public static final Supplier<Item> SMALL_ESSENCE = registerItem("small_essence", () -> new KnightLibItem(new Item.Properties(), "small_essence"));
    public static final Supplier<Item> GREAT_ESSENCE = registerItem("great_essence", () -> new KnightLibItem(new Item.Properties(), "great_essence"));

    private static <T extends Item> Supplier<T> registerItem(String id, Supplier<T> item) {
        return KnightLibCommon.COMMON_PLATFORM.registerItem(id, item);
    }

    private static class KnightLibItem extends Item {

        private final String tooltipInfoName;

        public KnightLibItem(Properties properties, String tooltipInfoName) {
            super(properties);
            this.tooltipInfoName = tooltipInfoName;
        }

        @Override
        public void appendHoverText(@NotNull ItemStack itemStack, @Nullable Level level, @NotNull List<Component> list, @NotNull TooltipFlag tooltipFlag) {

            list.add(Component.translatable("tooltip.item.knightlib." + tooltipInfoName));

            super.appendHoverText(itemStack, level, list, tooltipFlag);
        }
    }

}
