package dev.xylonity.knightlib.api.event.impl.client;

import dev.xylonity.knightlib.api.event.KnightLibEvent;
import dev.xylonity.knightlib.api.registrar.ResourceEntry;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

import java.util.function.Function;

/**
 * Event to register particle factories
 */
public abstract class ParticleProviderRegistrationEvent extends KnightLibEvent {

    @Override
    public boolean isSticky() {
        return true;
    }

    public <T extends ParticleOptions> void register(ResourceEntry<ParticleType<T>> particleEntry, Function<SpriteSet, ParticleProvider<T>> provider) {
        register(particleEntry.get(), provider);
    }

    public abstract <T extends ParticleOptions> void register(ParticleType<T> particleType, Function<SpriteSet, ParticleProvider<T>> provider);

}