package dev.xylonity.knightlib.compat.registry;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.KnightLibCommon;
import dev.xylonity.knightlib.compat.block.ChaliceBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class KnightLibBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, KnightLibCommon.MOD_ID);

    private static <I extends Block> DeferredHolder<Item, BlockItem> registerBlockItem(String name, DeferredHolder<Block, I> blockHolder) {
        return KnightLib.ITEMS.register(name, () -> new BlockItem(blockHolder.get(), new Item.Properties()));
    }

    private static <I extends Block> DeferredHolder<Block, I> registerBlock(String name, Supplier<I> blockSupplier) {
        DeferredHolder<Block, I> blockHolder = BLOCKS.register(name, blockSupplier);
        registerBlockItem(name, blockHolder);
        return blockHolder;
    }

    public static final DeferredHolder<Block, ChaliceBlock> GREAT_CHALICE = registerBlock("great_chalice",
            () -> new ChaliceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.COPPER)
                    .lightLevel(state ->
                            switch (state.getValue(ChaliceBlock.fill)) {
                                case 1 -> 1;
                                case 2, 9 -> 2;
                                case 3, 8 -> 4;
                                case 4, 7 -> 6;
                                case 5, 6 -> 8;
                                default -> 0;
                            }))
            {
                @Override
                public void appendHoverText(@NotNull ItemStack pStack, Item.@NotNull TooltipContext pContext, @NotNull List<Component> pTooltipComponents, @NotNull TooltipFlag pTooltipFlag) {
                    pTooltipComponents.add(Component.translatable("tooltip.item.knightlib.great_chalice"));
                    super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
                }
            });

}
