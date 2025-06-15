package dev.xylonity.knightlib.common.item;

import dev.xylonity.knightlib.common.api.GreatChaliceState;
import dev.xylonity.knightlib.common.api.IGreatChaliceInteractable;
import dev.xylonity.knightlib.common.blockentity.GreatChaliceBlockEntity;
import dev.xylonity.knightlib.registry.KnightLibItems;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class EmptyGrailItem extends Item implements IGreatChaliceInteractable {

    public EmptyGrailItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getChargesToApply() {
        return -MAX_CHARGES;
    }

    @Override
    public boolean canInteract(GreatChaliceBlockEntity chalice, Level level, Player player) {
        return chalice.isFull() &&
                (chalice.getState() == GreatChaliceState.NORMAL || chalice.getState() == GreatChaliceState.EMPTY);
    }

    @Override
    public @NotNull Set<ItemStack> getRewards() {
        return Set.of(new ItemStack(KnightLibItems.FILLED_GRAIL.get()));
    }

    @Override
    public @NotNull Set<SoundEvent> getInteractionSounds() {
        return Set.of(SoundEvents.BREWING_STAND_BREW);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, @Nullable Level level, @NotNull List<Component> list, @NotNull TooltipFlag flag) {
        list.add(Component.translatable("tooltip.item.knightlib.empty_grail"));
    }

}
