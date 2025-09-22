package dev.xylonity.knightlib.impl.internal;

import dev.xylonity.knightlib.api.bossbar.BossBarContext;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class BossBarApi {

    private static final List<BossBarEntry> ENTRIES = new ArrayList<>();

    private BossBarApi() { ;; }

    public static void register(BossBarEntry entry) {
        ENTRIES.add(entry);
    }

    public static Optional<BossBarEntry> match(LerpingBossEvent boss) {
        BossBarLinks.Ref reference = BossBarLinks.INSTANCE.get(boss.getId());
        Entity entity = reference != null ? reference.resolve() : null;
        BossBarContext ctx = new BossBarContext(boss, entity, reference != null ? reference.entityType : null);

        return ENTRIES
                .stream()
                .filter(
                        e -> {
                            if (e.matcher() != null) {
                                return e.matcher().test(ctx);
                            }

                            if (e.legacyMatcher() != null) {
                                return e.legacyMatcher().test(boss);
                            }

                            return false;
                    }
                )
                .findFirst();
    }

    public record BossBarEntry(Predicate<LerpingBossEvent> legacyMatcher, Predicate<BossBarContext> matcher, LegacyCustomBossBarRenderer legacyRenderer, CustomBossBarRenderer renderer, int extraYPadding, boolean hideVanillaName) {
        ;;
    }

}
