package dev.xylonity.knightlib.client.camera.editor;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Camera used while the path editor is open, so keyframes can be placed and manipulated from anywhere without moving the player
 */
public final class EditorFreeCamera {

    private static final float LOOK_SENSITIVITY = 0.15f;
    private static final float VELOCITY_SMOOTHING = 0.55f;

    private Vec3 position = Vec3.ZERO;
    private Vec3 prevPosition = Vec3.ZERO;
    private Vec3 velocity = Vec3.ZERO;

    private float yaw;
    private float pitch;

    private float speed = 0.6f;

    private boolean forward;
    private boolean back;
    private boolean left;
    private boolean right;
    private boolean up;
    private boolean down;
    private boolean sprint;

    public void reset(Vec3 position, float yaw, float pitch) {
        this.position = position;
        this.prevPosition = position;
        this.velocity = Vec3.ZERO;
        this.yaw = yaw;
        this.pitch = pitch;

        stopMoving();
    }

    public void setInput(boolean forward, boolean back, boolean left, boolean right, boolean up, boolean down, boolean sprint) {
        this.forward = forward;
        this.back = back;
        this.left = left;
        this.right = right;
        this.up = up;
        this.down = down;
        this.sprint = sprint;
    }

    public void stopMoving() {
        setInput(false, false, false, false, false, false, false);
        velocity = Vec3.ZERO;
    }

    public void tick() {
        prevPosition = position;

        final Vec3 wishedDirection = wishDirection();
        final float amount = speed * (sprint ? 3f : 1f);

        velocity = velocity.scale(VELOCITY_SMOOTHING).add(wishedDirection.scale(amount * (1f - VELOCITY_SMOOTHING)));
        if (velocity.lengthSqr() < 1.0E-6) {
            velocity = Vec3.ZERO;
        }

        position = position.add(velocity);
    }

    /**
     * Rotates the camera from raw mouse deltas
     */
    public void look(double dxPixels, double dyPixels) {
        yaw = Mth.wrapDegrees(yaw + (float) (dxPixels * LOOK_SENSITIVITY));
        pitch = Mth.clamp(pitch + (float) (dyPixels * LOOK_SENSITIVITY), -90f, 90f);
    }

    public void adjustSpeed(int sign) {
        speed = Mth.clamp(sign > 0 ? speed * 1.25f : speed / 1.25f, 0.02f, 10f);
    }

    public float speed() {
        return speed;
    }

    public Vec3 position(float partialTick) {
        return prevPosition.lerp(position, partialTick);
    }

    public Vec3 position() {
        return position;
    }

    public void moveTo(Vec3 position) {
        this.position = position;
        this.prevPosition = position;
        this.velocity = Vec3.ZERO;
    }

    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public Vec3 lookDirection() {
        return Vec3.directionFromRotation(pitch, yaw);
    }

    private Vec3 wishDirection() {
       final Vec3 forwardDir = lookDirection();
       final Vec3 rightDir = Vec3.directionFromRotation(0f, yaw + 90f);

        Vec3 wish = Vec3.ZERO;
        if (forward) {
            wish = wish.add(forwardDir);
        }
        if (back) {
            wish = wish.subtract(forwardDir);
        }
        if (right) {
            wish = wish.add(rightDir);
        }
        if (left) {
            wish = wish.subtract(rightDir);
        }
        if (up) {
            wish = wish.add(0, 1, 0);
        }
        if (down) {
            wish = wish.add(0, -1, 0);
        }

        return wish.lengthSqr() > 1.0 ? wish.normalize() : wish;
    }

}