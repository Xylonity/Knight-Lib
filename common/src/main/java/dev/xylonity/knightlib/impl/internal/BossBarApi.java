package dev.xylonity.knightlib.impl.internal;

import net.minecraft.client.gui.components.LerpingBossEvent;

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
        return ENTRIES.stream().filter(e -> e.matcher().test(boss)).findFirst();
    }

    public record BossBarEntry(Predicate<LerpingBossEvent> matcher, CustomBossBarRenderer renderer, int extraYPadding, boolean hideVanillaName) { ;; }

}