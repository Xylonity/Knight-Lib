package dev.xylonity.knightlib.client.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

public class KnightLibClientPlatformNeoForge implements KnightLibClientPlatform {

    @Override
    public BakedModel getAdditionalModel(ResourceLocation modelLocation) {
        return Minecraft.getInstance().getModelManager().getModel(new ModelResourceLocation(modelLocation, "standalone"));
    }

}
