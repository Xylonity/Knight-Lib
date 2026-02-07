package dev.xylonity.knightlib.client.event.impl;

import dev.xylonity.knightlib.api.event.impl.client.ParticleProviderRegistrationEvent;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ParticleProviderRegistrationEventForge extends ParticleProviderRegistrationEvent {

    private final Map<ParticleType<?>, ParticleEngine.SpriteParticleRegistration<?>> providers = new HashMap<>();

    @Override
    public <T extends ParticleOptions> void register(ParticleType<T> particleType, Function<SpriteSet, ParticleProvider<T>> provider) {
        providers.put(particleType, (ParticleEngine.SpriteParticleRegistration<T>) provider::apply);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void applyToForgeEvent(RegisterParticleProvidersEvent forgeEvent) {
        for (Map.Entry<ParticleType<?>, ParticleEngine.SpriteParticleRegistration<?>> entry : providers.entrySet()) {
            forgeEvent.registerSpriteSet(
                    (ParticleType) entry.getKey(),
                    (ParticleEngine.SpriteParticleRegistration) entry.getValue()
            );

        }

    }

}