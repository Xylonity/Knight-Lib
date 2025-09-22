package dev.xylonity.knightlib.impl.internal;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public final class BossBarLinks {

    public static final BossBarLinks INSTANCE = new BossBarLinks();
    private final Map<UUID, Ref> byBossId = new Object2ObjectOpenHashMap<>();

    public static final class Ref {
        public final int entityId;
        public final UUID entityUuid;
        public final ResourceLocation entityType;
        public final ResourceLocation dimension;

        public Ref(int id, UUID uuid, ResourceLocation type, ResourceLocation dimension) {
            this.entityId = id;
            this.entityUuid = uuid;
            this.entityType = type;
            this.dimension = dimension;
        }

        public @Nullable Entity resolve() {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.level == null) {
                return null;
            }

            if (!minecraft.level.dimension().location().equals(dimension)) {
                return null;
            }

            return minecraft.level.getEntity(entityId);
        }

    }

    public void put(UUID bossId, Ref ref) {
        byBossId.put(bossId, ref);
    }

    public @Nullable Ref get(UUID bossId) {
        return byBossId.get(bossId);
    }

    public void remove(UUID bossId) {
        byBossId.remove(bossId);
    }

    public void clear() {
        byBossId.clear();
    }

}
