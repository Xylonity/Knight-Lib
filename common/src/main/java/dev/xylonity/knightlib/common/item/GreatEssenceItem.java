package dev.xylonity.knightlib.common.item;

import dev.xylonity.knightlib.common.api.GreatChaliceState;
import dev.xylonity.knightlib.common.api.IGreatChaliceInteractable;
import net.minecraft.world.item.Item;

import java.util.Set;

public class GreatEssenceItem extends Item implements IGreatChaliceInteractable {

    public GreatEssenceItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getChargesToApply() {
        return 4;
    }

    @Override
    public Set<GreatChaliceState> getRequiredState() {
        return Set.of(GreatChaliceState.EMPTY, GreatChaliceState.NORMAL);
    }

}
