package dev.createport;

import dev.createport.block.KineticBlock;
import dev.createport.kinetics.RotationRules.Kind;
import dev.createport.block.KineticTile;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = CreatePort.ID, name = "Create Port Prototype", version = "0.2.0-dev", acceptedMinecraftVersions = "[1.12.2]")
@Mod.EventBusSubscriber(modid = CreatePort.ID)
public final class CreatePort {
    public static final String ID = "createport";
    public static final KineticBlock MOTOR = new KineticBlock("creative_motor", Kind.MOTOR);
    public static final KineticBlock SHAFT = new KineticBlock("shaft", Kind.SHAFT);
    public static final KineticBlock COG = new KineticBlock("cogwheel", Kind.SMALL);
    public static final KineticBlock LARGE_COG = new KineticBlock("large_cogwheel", Kind.LARGE);
    public static final KineticBlock[] BLOCKS = { MOTOR, SHAFT, COG, LARGE_COG };

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerTileEntity(KineticTile.class, new ResourceLocation(ID, "kinetic"));
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(BLOCKS);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (Block block : BLOCKS)
            event.getRegistry().register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }
}
