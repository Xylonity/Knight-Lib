package dev.xylonity.knightlib.common.entity.projectile;

import dev.xylonity.knightlib.common.entity.AbstractProjectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;

public class GreatChaliceStartsetRing extends AbstractProjectile implements GeoEntity {

    public GreatChaliceStartsetRing(EntityType<? extends AbstractProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void tick() {
        double px = getX();
        double py = getY();
        double pz = getZ();

        super.tick();

        this.setPos(px, py, pz);
    }

    @Override
    protected int baseLifetime() {
        return 10;
    }

}
