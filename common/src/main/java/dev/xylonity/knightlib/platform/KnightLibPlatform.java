package dev.xylonity.knightlib.platform;

import dev.xylonity.knightlib.api.armor.KnightLibArmorMaterial;
import dev.xylonity.knightlib.api.registrar.ResourceRegistry;
import dev.xylonity.knightlib.common.item.blockitem.GreatChaliceBlockItem;
import dev.xylonity.knightlib.registry.KnightLibBlockEntities;
import dev.xylonity.knightlib.registry.KnightLibBlocks;
import dev.xylonity.knightlib.registry.KnightLibItems;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface KnightLibPlatform {
    <T extends ParticleType<?>> Supplier<T> createParticle(boolean overrideLimiter);

    ArmorItem createArmorItem(KnightLibArmorMaterial material, ArmorItem.Type type, Item.Properties properties, ResourceLocation modelId);

    <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(ResourceRegistry.BlockEntityFactory<T> factory, Supplier<Block> block);
    <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(ResourceRegistry.BlockEntityFactory<T> factory, Supplier<Block>... blocks);
    <T extends AbstractContainerMenu> MenuType<T> createMenuFactory(ResourceRegistry.MenuFactory<T> supplier);
    <X extends Mob> Item createSpawnEgg(Supplier<EntityType<X>> entityType, int primaryColor, int secondaryColor, Item.Properties properties);

    void openMenu(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraData);

    boolean isPhysicalClient();
    boolean isModLoaded(String modid);
    boolean isDevelopmentEnvironment();

    Path resolveConfigFile(String configFileName);
    Path getConfigPath();

    CreativeModeTab.Builder creativeTabBuilder();
}
