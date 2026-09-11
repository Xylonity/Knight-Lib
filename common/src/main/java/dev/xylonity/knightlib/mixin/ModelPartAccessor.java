package dev.xylonity.knightlib.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.List;

@Mixin(ModelPart.class)
public interface ModelPartAccessor {

    @Accessor("children")
    Map<String, ModelPart> knightlib$getChildren();

    @Accessor("cubes")
    List<ModelPart.Cube> knightlib$getCubes();

}
