package dev.createport;

import dev.createport.block.KineticBlock;
import dev.createport.kinetics.RotationRules.Kind;
import dev.createport.block.KineticTile;
import dev.createport.block.MillstoneTile;
import dev.createport.block.TransmissionBlock;
import dev.createport.block.DirectionalGeneratorBlock;
import dev.createport.block.AndesiteAlloyItem;
import dev.createport.block.MillingRecipes;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = CreatePort.ID, name = "Create Port Prototype", version = "0.3.0-dev", acceptedMinecraftVersions = "[1.12.2]")
@Mod.EventBusSubscriber(modid = CreatePort.ID)
public final class CreatePort {
    public static final String ID = "createport";
    public static final KineticBlock MOTOR = new KineticBlock("creative_motor", Kind.MOTOR);
    public static final KineticBlock SHAFT = new KineticBlock("shaft", Kind.SHAFT);
    public static final KineticBlock COG = new KineticBlock("cogwheel", Kind.SMALL);
    public static final KineticBlock LARGE_COG = new KineticBlock("large_cogwheel", Kind.LARGE);
    public static final KineticBlock CLUTCH = new TransmissionBlock("clutch", Kind.CLUTCH);
    public static final KineticBlock GEARSHIFT = new TransmissionBlock("gearshift", Kind.GEARSHIFT);
    public static final KineticBlock GEARBOX = new KineticBlock("gearbox", Kind.GEARBOX);
    public static final KineticBlock ANDESITE_SHAFT = new KineticBlock("andesite_encased_shaft", Kind.ENCASED);
    public static final KineticBlock BRASS_SHAFT = new KineticBlock("brass_encased_shaft", Kind.ENCASED);
    public static final KineticBlock CRANK = new DirectionalGeneratorBlock("hand_crank", Kind.CRANK);
    public static final KineticBlock WATER_WHEEL = new DirectionalGeneratorBlock("water_wheel", Kind.WATER_WHEEL);
    public static final KineticBlock MILLSTONE = new KineticBlock("millstone", Kind.MILLSTONE);
    public static final Block ANDESITE_CASING = new Block(Material.WOOD).setRegistryName(ID, "andesite_casing").setUnlocalizedName(ID + ".andesite_casing").setHardness(1.5f).setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    public static final Block[] BLOCKS = { MOTOR, SHAFT, COG, LARGE_COG, CLUTCH, GEARSHIFT, GEARBOX, ANDESITE_SHAFT, BRASS_SHAFT, CRANK, WATER_WHEEL, MILLSTONE, ANDESITE_CASING };
    public static final Item FLOUR = new Item().setRegistryName(ID, "wheat_flour").setUnlocalizedName(ID + ".wheat_flour").setCreativeTab(CreativeTabs.MATERIALS);
    public static final Item DOUGH = new Item().setRegistryName(ID, "dough").setUnlocalizedName(ID + ".dough").setCreativeTab(CreativeTabs.MATERIALS);
    public static final Item ANDESITE_ALLOY = new AndesiteAlloyItem().setRegistryName(ID, "andesite_alloy").setUnlocalizedName(ID + ".andesite_alloy").setCreativeTab(CreativeTabs.MATERIALS);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerTileEntity(KineticTile.class, new ResourceLocation(ID, "kinetic"));
        GameRegistry.registerTileEntity(MillstoneTile.class, new ResourceLocation(ID, "millstone"));
    }

    @Mod.EventHandler public void init(FMLInitializationEvent event) {
        GameRegistry.addSmelting(DOUGH, new ItemStack(Items.BREAD), 0.35f);
        MillingRecipes.load();
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(BLOCKS);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (Block block : BLOCKS)
            event.getRegistry().register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
        event.getRegistry().registerAll(FLOUR, DOUGH, ANDESITE_ALLOY);
    }

    @SubscribeEvent public static void recipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new ShapedOreRecipe(null, new ItemStack(ANDESITE_ALLOY), "IA", "AI", 'I', Items.IRON_NUGGET, 'A', new ItemStack(Blocks.STONE, 1, 5)).setRegistryName(ID, "andesite_alloy"));
        event.getRegistry().register(new ShapedOreRecipe(null, new ItemStack(SHAFT, 8), "A", "A", 'A', ANDESITE_ALLOY).setRegistryName(ID, "shaft"));
        event.getRegistry().register(new ShapelessOreRecipe(null, new ItemStack(COG), SHAFT, "plankWood").setRegistryName(ID, "cogwheel"));
        event.getRegistry().register(new ShapelessOreRecipe(null, new ItemStack(LARGE_COG), SHAFT, "plankWood", "plankWood").setRegistryName(ID, "large_cogwheel"));
        event.getRegistry().register(new ShapedOreRecipe(null, new ItemStack(CRANK), "PPP", "  A", 'P', "plankWood", 'A', ANDESITE_ALLOY).setRegistryName(ID, "hand_crank"));
        event.getRegistry().register(new ShapedOreRecipe(null, new ItemStack(WATER_WHEEL), "PPP", "PSP", "PPP", 'P', "plankWood", 'S', SHAFT).setRegistryName(ID, "water_wheel"));
        event.getRegistry().register(new ShapelessOreRecipe(null, new ItemStack(CLUTCH), ANDESITE_CASING, SHAFT, "dustRedstone").setRegistryName(ID, "clutch"));
        event.getRegistry().register(new ShapelessOreRecipe(null, new ItemStack(GEARSHIFT), ANDESITE_CASING, COG, "dustRedstone").setRegistryName(ID, "gearshift"));
        event.getRegistry().register(new ShapedOreRecipe(null, new ItemStack(GEARBOX), " C ", "CBC", " C ", 'C', COG, 'B', ANDESITE_CASING).setRegistryName(ID, "gearbox"));
        event.getRegistry().register(new ShapedOreRecipe(null, new ItemStack(MILLSTONE), "C", "B", "S", 'C', COG, 'B', ANDESITE_CASING, 'S', "stone").setRegistryName(ID, "millstone"));
        event.getRegistry().register(new ShapelessOreRecipe(null, new ItemStack(DOUGH), FLOUR, Items.WATER_BUCKET).setRegistryName(ID, "dough"));
        event.getRegistry().register(new ShapelessOreRecipe(null, new ItemStack(ANDESITE_SHAFT), SHAFT, ANDESITE_CASING).setRegistryName(ID, "andesite_encased_shaft"));
    }
}
