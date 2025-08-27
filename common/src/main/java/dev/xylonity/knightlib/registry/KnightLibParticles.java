package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLib;
import dev.xylonity.knightlib.api.registrar.ResourceDispatcher;
import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.api.registrar.ResourceType;
import net.minecraft.core.particles.SimpleParticleType;

public class KnightLibParticles {

    public static final ResourceRegistry<SimpleParticleType> PARTICLES = ResourceDispatcher.create(ResourceType.PARTICLES, KnightLib.MOD_ID);

    public static final ResourceEntry<SimpleParticleType> STARSET = PARTICLES.register("starset", KnightLib.PLATFORM.createParticle(true));

}
