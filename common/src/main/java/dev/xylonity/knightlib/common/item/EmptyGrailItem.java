package dev.xylonity.knightlib.common.item;

import dev.xylonity.knightlib.common.api.GreatChaliceState;
import dev.xylonity.knightlib.common.api.IGreatChaliceInteractable;
import dev.xylonity.knightlib.registry.KnightLibItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

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
    public int getRequiredCharges() {
        return MAX_CHARGES;
    }

    @Override
    public @NotNull Set<ItemStack> getRewards() {
        return Set.of(new ItemStack(KnightLibItems.FILLED_GRAIL.get()));
    }

    @Override
    public Set<GreatChaliceState> getRequiredState() {
        return Set.of(GreatChaliceState.EMPTY, GreatChaliceState.NORMAL);
    }

}
