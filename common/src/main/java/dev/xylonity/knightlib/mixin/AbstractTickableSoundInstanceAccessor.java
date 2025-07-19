package dev.xylonity.knightlib.mixin;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractTickableSoundInstance.class)
public interface AbstractTickableSoundInstanceAccessor {

    @Invoker("stop")
    void stopAccessor();

}
