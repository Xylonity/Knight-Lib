package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLibCommon;
import dev.xylonity.knightlib.registry.registrar.ResourceDispatcher;
import dev.xylonity.knightlib.registry.registrar.ResourceEntry;
import dev.xylonity.knightlib.registry.registrar.ResourceRegistry;
import dev.xylonity.knightlib.registry.registrar.ResourceType;
import net.minecraft.core.particles.SimpleParticleType;

public class KnightLibParticles {

    public static final ResourceRegistry<SimpleParticleType> PARTICLES = ResourceDispatcher.create(ResourceType.PARTICLES, KnightLibCommon.MOD_ID);

    public static final ResourceEntry<SimpleParticleType> STARSET = PARTICLES.register("starset", KnightLibCommon.PLATFORM.createParticle(true));

}
