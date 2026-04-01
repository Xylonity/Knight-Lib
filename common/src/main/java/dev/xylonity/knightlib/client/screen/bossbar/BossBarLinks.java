package dev.xylonity.knightlib.client.screen.bossbar;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public final class BossBarLinks {

    public static final BossBarLinks INSTANCE = new BossBarLinks();
    private final Map<UUID, Reference> byBossId = new Object2ObjectOpenHashMap<>();

    public record Reference(
            int entityId,
            UUID entityUuid,
            ResourceLocation entityType,
            ResourceLocation dimension
    ) {

        public @Nullable Entity resolve() {
            final Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.level == null) {
                return null;
            }

            if (!minecraft.level.dimension().location().equals(dimension)) {
                return null;
            }

            final Entity entity = minecraft.level.getEntity(entityId);
            if (entity != null && entity.getUUID().equals(entityUuid)) {
                return entity;
            }

            return null;
        }

    }

    public void put(UUID bossId, Reference reference) {
        byBossId.put(bossId, reference);
    }

    public @Nullable BossBarLinks.Reference get(UUID bossId) {
        return byBossId.get(bossId);
    }

    public void remove(UUID bossId) {
        byBossId.remove(bossId);
    }

    public void clear() {
        byBossId.clear();
    }

}
