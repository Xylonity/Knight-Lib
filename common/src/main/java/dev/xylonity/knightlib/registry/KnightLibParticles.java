package dev.xylonity.knightlib.registry;

import dev.xylonity.knightlib.KnightLibCommon;
import net.minecraft.core.particles.SimpleParticleType;

import java.util.function.Supplier;

public class KnightLibParticles {

    public static void init() { ;; }

    public static final Supplier<SimpleParticleType> STARSET = registerParticle("starset", true);

    private static <T extends SimpleParticleType> Supplier<T> registerParticle(String id, boolean overrideLimiter) {
        return KnightLibCommon.PLATFORM.registerParticle(id, overrideLimiter);
    }

}
