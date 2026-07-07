package dev.xylonity.knightlib.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {

    @Invoker("move")
    void moveAccessor(double dx, double dy, double dz);

    @Invoker("setPosition")
    void setPositionAccessor(double x, double y, double z);

    @Invoker("setRotation")
    void setRotationAccessor(float yRot, float xRot);

    @Accessor("detached")
    void setDetachedAccessor(boolean detached);

}