package dev.xylonity.knightlib.common.event;

import dev.xylonity.knightlib.KnightLibCommon;
import dev.xylonity.knightlib.datagen.KnightLibLootModifierGenerator;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KnightLibCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KnightLibServerEvents {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();

        generator.addProvider(event.includeServer(), new KnightLibLootModifierGenerator(packOutput, event.getLookupProvider()));
    }

}