package dev.xylonity.knightlib.mixin;

import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.EffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PostPass.class)
public interface PostPassAccessor {

    @Accessor("effect")
    EffectInstance knightlib$getEffect();

}
