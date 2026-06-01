package dev.xylonity.knightlib.client.screen.bossbar;

import dev.xylonity.knightlib.api.bossbar.BossBarContext;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class BossBarApi {

    private static final List<BossBarEntry> ENTRIES = new ArrayList<>();

    private BossBarApi() {
        ;;
    }

    public static void register(BossBarEntry entry) {
        ENTRIES.add(entry);
    }

    public static Optional<BossBarEntry> match(LerpingBossEvent boss) {
        final BossBarLinks.Reference reference = BossBarLinks.INSTANCE.get(boss.getId());
        final Entity entity = reference != null ? reference.resolve() : null;
        final BossBarContext context = new BossBarContext(boss, entity, reference != null ? reference.entityType() : null);

        return ENTRIES
                .stream()
                .filter(
                        bossBarEntry -> {
                            if (bossBarEntry.matcher() != null) {
                                return bossBarEntry.matcher().test(context);
                            }

                            if (bossBarEntry.legacyMatcher() != null) {
                                return bossBarEntry.legacyMatcher().test(boss);
                            }

                            return false;
                    }
                )
                .findFirst();
    }

    public record BossBarEntry(
            Predicate<LerpingBossEvent> legacyMatcher,
            Predicate<BossBarContext> matcher,
            LegacyCustomBossBarRenderer legacyRenderer,
            CustomBossBarRenderer renderer,
            int extraYPadding,
            boolean hideVanillaName
    ) {
        ;;
    }

}
