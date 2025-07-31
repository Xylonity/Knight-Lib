package dev.xylonity.knightlib.common.item;

import dev.xylonity.knightlib.api.IGreatChaliceInteractable;
import dev.xylonity.knightlib.api.impl.GreatChaliceState;
import dev.xylonity.knightlib.common.blockentity.GreatChaliceBlockEntity;
import dev.xylonity.knightlib.registry.KnightLibItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
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
        return chalice.isFull() && chalice.getState() == GreatChaliceState.NORMAL;
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
    public void onPostInteraction(GreatChaliceBlockEntity chalice, Player player, Level level, BlockHitResult hit) {
        if (level instanceof ServerLevel sv) {
            RandomSource rand = sv.getRandom();
            for (int i = 0; i < 25; i++) {
                double px, py, pz;
                double r = 0.7 * Math.sqrt(rand.nextDouble());
                double t = rand.nextDouble() * Math.PI * 2;
                px = chalice.getBlockPos().getX() + 0.5 + r * Math.cos(t);
                pz = chalice.getBlockPos().getZ() + 0.5 + r * Math.sin(t);
                py = chalice.getBlockPos().getY() + 1.0 + rand.nextDouble() * 0.3;
                sv.sendParticles(ParticleTypes.EFFECT, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, @Nullable Level level, @NotNull List<Component> list, @NotNull TooltipFlag flag) {
        super.appendHoverText(itemStack, level, list, flag);
        list.add(Component.translatable("tooltip.item.knightlib.empty_grail"));
    }

}
