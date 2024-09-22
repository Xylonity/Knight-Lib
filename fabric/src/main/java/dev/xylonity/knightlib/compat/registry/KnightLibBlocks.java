package dev.xylonity.knightlib.compat.registry;

import dev.xylonity.knightlib.KnightLibCommon;
import dev.xylonity.knightlib.compat.block.ChaliceBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class KnightLibBlocks {

    public static void register() { }

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(KnightLibCommon.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block) {
        return Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(KnightLibCommon.MOD_ID, name), new BlockItem(block, new Item.Properties()));
    }

    public static final Block GREAT_CHALICE = registerBlock("great_chalice",
            new ChaliceBlock(FabricBlockSettings.create().nonOpaque().mapColor(MapColor.COLOR_ORANGE).requiresTool().strength(2.0F).sounds(SoundType.COPPER)
                    .luminance(state -> switch (state.getValue(ChaliceBlock.fill)) {
                        case 1 -> 1;
                        case 2, 9 -> 2;
                        case 3, 8 -> 4;
                        case 4, 7 -> 6;
                        case 5, 6 -> 8;
                        default -> 0;
                    }))
            {
                @Override
                public void appendHoverText(@NotNull ItemStack pStack, @Nullable BlockGetter pLevel, @NotNull List<Component> pTooltip, @NotNull TooltipFlag pFlag) {
                    pTooltip.add(Component.translatable("tooltip.item.knightlib.great_chalice"));
                    super.appendHoverText(pStack, pLevel, pTooltip, pFlag);
                }
            }
    );

}
