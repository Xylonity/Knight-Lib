package dev.xylonity.knightlib;

import net.minecraftforge.fml.common.Mod;

@Mod(KnightLibConstants.MOD_ID)
public class KnightLib {
    
    public KnightLib() {
        KnightLibCommon.init();
    }
}