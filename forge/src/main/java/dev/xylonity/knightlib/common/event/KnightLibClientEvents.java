package dev.xylonity.knightlib.common.event;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.client.blockentity.renderer.GreatChaliceRenderer;
import dev.xylonity.knightlib.client.projectile.renderer.GreatChaliceStarsetRingRenderer;
import dev.xylonity.knightlib.common.particle.StarsetParticle;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibEntities;
import dev.xylonity.knightlib.registry.KnightLibParticles;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = KnightLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KnightLibClientEvents {

    @SubscribeEvent
    public static void registerEntityRenderers(FMLClientSetupEvent event) {
        EntityRenderers.register(KnightLibEntities.GREAT_CHALICE_STARSET_RING.get(), GreatChaliceStarsetRingRenderer::new);

        BlockEntityRenderers.register(KnightLibBlockEntities.GREAT_CHALICE.get(), GreatChaliceRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleFactories(final RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(KnightLibParticles.STARSET.get(), StarsetParticle.Provider::new);
    }

}
