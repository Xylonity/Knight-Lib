package dev.xylonity.knightlib;

import net.fabricmc.api.ModInitializer;

public class KnightLib implements ModInitializer {
    
    @Override
    public void onInitialize() {
        KnightLibCommon.init();
    }
}
