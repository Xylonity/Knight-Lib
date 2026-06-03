package dev.xylonity.knightlib.client.platform;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

public interface KnightLibClientPlatform {

    BakedModel getAdditionalModel(ResourceLocation modelLocation);

}
