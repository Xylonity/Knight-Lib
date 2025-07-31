package dev.xylonity.knightlib.registry.registrar;

/**
 * Every ResourceType maps to one Registry on the service loader abstraction
 */
public enum ResourceType {
    BLOCKS,
    ITEMS,
    ENTITIES,
    BLOCK_ENTITIES,
    EFFECTS,
    SOUNDS,
    PARTICLES,
    CREATIVE_TAB,
    MENU,
    ENCHANTMENTS,
    DIMENSION_TYPE,
    DIMENSION,
    FLUID,
    BIOME,
    BIOME_SOURCE,
    DAMAGE_TYPE,
    STRUCTURE,
    STRUCTURE_PIECE,
    STRUCTURE_TYPE,
    STRUCTURE_PLACEMENT,
    STRUCTURE_SET,
    STRUCTURE_POOL_ELEMENT,
    STRUCTURE_PROCESSOR,
    SENSOR_TYPE
}
