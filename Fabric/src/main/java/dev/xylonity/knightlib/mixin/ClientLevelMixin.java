package dev.xylonity.knightlib.mixin;

import dev.xylonity.knightlib.api.IBossMusicProvider;
import dev.xylonity.knightlib.api.impl.BossMusicRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Inject(method = "addEntity(ILnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void onAddEntity(int id, Entity entity, CallbackInfo ci) {
        if (entity instanceof IBossMusicProvider prov) {
            BossMusicRegistry.register(prov);
        }
    }

    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void onRemoveEntity(int entityId, Entity.RemovalReason reason, CallbackInfo ci) {
        BossMusicRegistry.unregisterById(entityId);
    }

}
