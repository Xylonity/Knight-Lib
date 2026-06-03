package dev.xylonity.knightlib.client.platform;

import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;

public class KnightLibClientPlatformFabric implements KnightLibClientPlatform {

    @Override
    public BakedModel getAdditionalModel(ResourceLocation modelLocation) {
        final ModelManager modelManager = Minecraft.getInstance().getModelManager();
        final BakedModel model = ((FabricBakedModelManager) modelManager).getModel(modelLocation);
        return model != null ? model : modelManager.getMissingModel();
    }

}
