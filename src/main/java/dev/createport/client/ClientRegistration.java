package dev.createport.client;

import dev.createport.CreatePort;
import dev.createport.block.KineticTile;
import dev.createport.block.KineticBlock;
import dev.createport.block.DirectionalGeneratorBlock;
import dev.createport.kinetics.RotationRules.Kind;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = CreatePort.ID, value = Side.CLIENT)
public final class ClientRegistration {
    private static boolean initialized;
    @SubscribeEvent
    public static void models(ModelRegistryEvent event) {
        if (!initialized) {
            OBJLoader.INSTANCE.addDomain("create");
            ModelLoaderRegistry.registerLoader(new LegacyObjLoader());
            MinecraftForge.EVENT_BUS.register(ModelCatalog.INSTANCE);
            ClientRegistry.bindTileEntitySpecialRenderer(KineticTile.class, new KineticRenderer());
            initialized = true;
        }
        for (Block block : CreatePort.BLOCKS) {
            if (block instanceof DirectionalGeneratorBlock)
                ModelLoader.setCustomStateMapper(block, new StateMap.Builder().ignore(KineticBlock.AXIS).build());
            else if (block == CreatePort.MILLSTONE)
                ModelLoader.setCustomStateMapper(block, new StateMap.Builder().ignore(KineticBlock.AXIS).build());
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
                new ModelResourceLocation(block.getRegistryName(), "inventory"));
        }
        for (Item item : new Item[] { CreatePort.FLOUR, CreatePort.DOUGH, CreatePort.ANDESITE_ALLOY })
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
