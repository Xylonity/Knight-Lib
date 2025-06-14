package dev.xylonity.knightlib.common.item;

import dev.xylonity.knightlib.common.api.GreatChaliceState;
import dev.xylonity.knightlib.common.api.IGreatChaliceInteractable;
import net.minecraft.world.item.Item;

import java.util.Set;

public class SmallEssenceItem extends Item implements IGreatChaliceInteractable {

    public SmallEssenceItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getChargesToApply() {
        return 1;
    }

    @Override
    public Set<GreatChaliceState> getRequiredState() {
        return Set.of(GreatChaliceState.EMPTY, GreatChaliceState.NORMAL);
    }

}
