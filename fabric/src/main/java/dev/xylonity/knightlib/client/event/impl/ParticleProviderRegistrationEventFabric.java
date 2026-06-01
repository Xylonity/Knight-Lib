package dev.xylonity.knightlib.client.event.impl;

import dev.xylonity.knightlib.api.event.impl.client.ParticleProviderRegistrationEvent;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

import java.util.function.Function;

public class ParticleProviderRegistrationEventFabric extends ParticleProviderRegistrationEvent {

    @Override
    public <T extends ParticleOptions> void register(ParticleType<T> particleType, Function<SpriteSet, ParticleProvider<T>> provider) {
        ParticleFactoryRegistry.getInstance().register(particleType, provider::apply);
    }

}