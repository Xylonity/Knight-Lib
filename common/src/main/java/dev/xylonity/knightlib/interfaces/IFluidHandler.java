package dev.xylonity.knightlib.interfaces;

public interface IFluidHandler {

    /**
     * Fills the block or entity with a fluid.
     *
     * @param fluidAmount The amount of fluid to fill.
     * @return The amount of fluid that was successfully filled.
     */
    int fill(int fluidAmount);

    /**
     * Drains fluid from the block or entity.
     *
     * @param fluidAmount The amount of fluid to drain.
     * @return The amount of fluid that was actually drained.
     */
    int drain(int fluidAmount);

    /**
     * Gets the current amount of fluid stored in the block or entity.
     *
     * @return The current fluid level.
     */
    int getFluidLevel();

    /**
     * Gets the maximum amount of fluid the block or entity can store.
     *
     * @return The maximum fluid capacity.
     */
    int getMaxFluidCapacity();

}
