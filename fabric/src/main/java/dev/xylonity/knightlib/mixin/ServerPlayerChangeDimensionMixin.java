package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.event.KnightLibEvents;
import dev.xylonity.knightlib.api.event.impl.server.PlayerChangedDimensionEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerChangeDimensionMixin {

    @Inject(method = "changeDimension", at = @At("RETURN"))
    private void knightlib$afterChangeDimension(ServerLevel destination, CallbackInfoReturnable<Entity> cir) {
        final ServerPlayer self = (ServerPlayer) (Object) this;
        if (cir.getReturnValue() == null) {
            return;
        }

        final ResourceKey<Level> origin = self.level().dimension();
        KnightLibEvents.SERVER.dispatch(new PlayerChangedDimensionEvent(
                self.server, self, origin, destination.dimension()
        ));
    }

}