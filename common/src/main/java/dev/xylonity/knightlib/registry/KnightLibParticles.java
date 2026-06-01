package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.registrar.ResourceDispatcher;
import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public class KnightLibParticles {

    public static final ResourceRegistry<ParticleType<?>> PARTICLES = ResourceDispatcher.create(BuiltInRegistries.PARTICLE_TYPE, KnightLib.MOD_ID);

    public static final ResourceEntry<SimpleParticleType> STARSET = PARTICLES.register("starset", KnightLib.PLATFORM.createParticle(true));

}
