package dev.createport.client;

import com.google.gson.GsonBuilder;
import dev.createport.CreatePort;
import dev.createport.block.DirectionalGeneratorBlock;
import dev.createport.block.KineticBlock;
import dev.createport.block.KineticTile;
import dev.createport.block.MillstoneTile;
import dev.createport.block.MillingRecipes;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.block.BlockLiquid;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/** Real world/capability checks for the power, transmission and milling milestone. */
final class EnergySmoke {
    private static int stage, ticks;
    private static final Map<String, Object> result = new LinkedHashMap<>();
    private static final BlockPos CRANK = new BlockPos(10, 65, 0), MILL = new BlockPos(10, 67, 0), OVERLOAD = new BlockPos(18, 69, 2);
    static boolean run(Minecraft mc) throws Exception {
        if (stage == 0) {
            mc.getIntegratedServer().addScheduledTask(() -> setup(mc)).get();
            stage = 1; return false;
        }
        if (stage == 1) {
            mc.getIntegratedServer().addScheduledTask(() -> {
                WorldServer world = mc.getIntegratedServer().getWorld(0);
                tile(world, CRANK).crank(false);
                tile(world, new BlockPos(16, 65, 0)).crank(false);
            }).get();
            if (++ticks < 200) return false;
            mc.getIntegratedServer().addScheduledTask(() -> verify(mc)).get();
            result.put("passed", true);
            Files.write(Paths.get("energy-smoke.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result).getBytes(StandardCharsets.UTF_8));
            stage = 2; ticks = 0; return false;
        }
        if (stage == 2) {
            mc.getIntegratedServer().addScheduledTask(() -> {
                WorldServer world = mc.getIntegratedServer().getWorld(0);
                tile(world, CRANK).crank(false);
                tile(world, new BlockPos(16, 65, 0)).crank(false);
            }).get();
            if (++ticks < 20) return false;
            RenderSmoke.capture("createport-energy.png"); stage = 3; return false;
        }
        return true;
    }
    private static KineticTile tile(WorldServer world, BlockPos pos) { return (KineticTile) world.getTileEntity(pos); }
    private static void place(WorldServer world, KineticBlock block, int x, int y, int z, EnumFacing.Axis axis) {
        world.setBlockState(new BlockPos(x, y, z), block.getDefaultState().withProperty(KineticBlock.AXIS, axis));
    }
    private static void crank(WorldServer world, int x, int y, int z) {
        world.setBlockState(new BlockPos(x, y, z), CreatePort.CRANK.getDefaultState().withProperty(DirectionalGeneratorBlock.FACING, EnumFacing.DOWN).withProperty(KineticBlock.AXIS, EnumFacing.Axis.Y));
    }
    private static void expect(double actual, double expected, String label) {
        if (Math.abs(actual - expected) > 0.001) throw new AssertionError(label + ": " + actual + " expected " + expected);
    }
    private static void setup(Minecraft mc) {
        WorldServer w = mc.getIntegratedServer().getWorld(0);
        if (CreatePort.MILLSTONE.getStateFromMeta(0).getValue(KineticBlock.AXIS) != EnumFacing.Axis.Y) throw new AssertionError("Millstone metadata must preserve a vertical axle");
        for (int x = 9; x <= 30; x++) for (int z = -1; z <= 7; z++) {
            w.setBlockState(new BlockPos(x, 64, z), Blocks.STONEBRICK.getDefaultState());
            for (int y = 65; y <= 72; y++) w.setBlockToAir(new BlockPos(x, y, z));
        }
        crank(w, 10, 65, 0); place(w, CreatePort.SHAFT, 10, 66, 0, EnumFacing.Axis.Y); place(w, CreatePort.MILLSTONE, 10, 67, 0, EnumFacing.Axis.Y);
        crank(w, 16, 65, 0); place(w, CreatePort.LARGE_COG, 16, 66, 0, EnumFacing.Axis.Y);
        place(w, CreatePort.COG, 17, 66, 1, EnumFacing.Axis.Y); place(w, CreatePort.LARGE_COG, 17, 67, 1, EnumFacing.Axis.Y);
        place(w, CreatePort.COG, 18, 67, 2, EnumFacing.Axis.Y); place(w, CreatePort.SHAFT, 18, 68, 2, EnumFacing.Axis.Y); place(w, CreatePort.MILLSTONE, 18, 69, 2, EnumFacing.Axis.Y);
        place(w, CreatePort.MOTOR, 10, 65, 5, EnumFacing.Axis.X); place(w, CreatePort.GEARSHIFT, 11, 65, 5, EnumFacing.Axis.X); place(w, CreatePort.SHAFT, 12, 65, 5, EnumFacing.Axis.X);
        place(w, CreatePort.MOTOR, 16, 65, 5, EnumFacing.Axis.X); place(w, CreatePort.CLUTCH, 17, 65, 5, EnumFacing.Axis.X); place(w, CreatePort.SHAFT, 18, 65, 5, EnumFacing.Axis.X);
        place(w, CreatePort.MOTOR, 22, 65, 5, EnumFacing.Axis.X); place(w, CreatePort.GEARBOX, 23, 65, 5, EnumFacing.Axis.Y);
        place(w, CreatePort.SHAFT, 24, 65, 5, EnumFacing.Axis.X); place(w, CreatePort.SHAFT, 23, 65, 6, EnumFacing.Axis.Z);
        tile(w, new BlockPos(10, 65, 5)).refreshNetwork(); tile(w, new BlockPos(16, 65, 5)).refreshNetwork(); tile(w, new BlockPos(22, 65, 5)).refreshNetwork();
        expect(tile(w, new BlockPos(12, 65, 5)).getSpeed(), 16, "Gearshift normal");
        expect(tile(w, new BlockPos(18, 65, 5)).getSpeed(), 16, "Clutch engaged");
        expect(tile(w, new BlockPos(24, 65, 5)).getSpeed(), -16, "Gearbox opposite port");
        expect(tile(w, new BlockPos(23, 65, 6)).getSpeed(), 16, "Gearbox corner port");
        w.setBlockState(new BlockPos(11, 64, 5), Blocks.REDSTONE_BLOCK.getDefaultState());
        w.setBlockState(new BlockPos(17, 64, 5), Blocks.REDSTONE_BLOCK.getDefaultState());
        expect(tile(w, new BlockPos(12, 65, 5)).getSpeed(), -16, "Gearshift redstone reverse");
        expect(tile(w, new BlockPos(18, 65, 5)).getSpeed(), 0, "Powered clutch output");
        expect(tile(w, new BlockPos(16, 65, 5)).getSpeed(), 16, "Disconnected input source");
        w.setBlockState(new BlockPos(17, 64, 5), Blocks.STONEBRICK.getDefaultState());
        expect(tile(w, new BlockPos(18, 65, 5)).getSpeed(), 16, "Clutch recovery");
        result.put("redstone_clutch_gearshift_and_gearbox", true);
        IItemHandler input = tile(w, MILL).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        if (!input.insertItem(0, new ItemStack(Items.WHEAT), true).isEmpty() || !input.getStackInSlot(0).isEmpty()) throw new AssertionError("Simulated insert changed inventory");
        if (!input.insertItem(0, new ItemStack(Items.WHEAT), false).isEmpty()) throw new AssertionError("Mill refused wheat");
        if (!input.extractItem(0, 1, false).isEmpty()) throw new AssertionError("Input extraction should be blocked");
        IItemHandler overloadedInput = tile(w, OVERLOAD).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        overloadedInput.insertItem(0, new ItemStack(Items.WHEAT), false);
        // Still water is not a source; falling water exerts torque around the axle.
        BlockPos wheel = new BlockPos(28, 67, 0);
        w.setBlockState(wheel, CreatePort.WATER_WHEEL.getDefaultState().withProperty(DirectionalGeneratorBlock.FACING, EnumFacing.EAST).withProperty(KineticBlock.AXIS, EnumFacing.Axis.X));
        place(w, CreatePort.SHAFT, 29, 67, 0, EnumFacing.Axis.X);
        expect(tile(w, wheel).getGeneratedSpeed(), 0, "Dry wheel");
        w.setBlockState(new BlockPos(29, 67, 1), Blocks.STONE.getDefaultState());
        w.setBlockState(new BlockPos(28, 67, 1), Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, 8));
        tile(w, wheel).refreshNetwork();
        expect(Math.abs(tile(w, wheel).getSpeed()), 8, "Flow-driven wheel RPM");
        expect(tile(w, wheel).getCapacity(), 256, "Wheel stress capacity");
        expect(Math.abs(tile(w, new BlockPos(29, 67, 0)).getSpeed()), 8, "Wheel output shaft");
        w.setBlockToAir(new BlockPos(28, 67, 1)); tile(w, wheel).refreshNetwork();
        expect(tile(w, wheel).getSpeed(), 0, "Dry wheel stops");
        result.put("flow_driven_waterwheel", true);
        InventoryCrafting grid = new InventoryCrafting(new Container() { @Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer p) { return false; } }, 3, 3);
        grid.setInventorySlotContents(0, new ItemStack(Items.IRON_NUGGET)); grid.setInventorySlotContents(1, new ItemStack(Blocks.STONE, 1, 5));
        grid.setInventorySlotContents(3, new ItemStack(Blocks.STONE, 1, 5)); grid.setInventorySlotContents(4, new ItemStack(Items.IRON_NUGGET));
        if (CraftingManager.findMatchingResult(grid, w).getItem() != CreatePort.ANDESITE_ALLOY) throw new AssertionError("Alloy crafting recipe");
        grid.clear(); grid.setInventorySlotContents(0, new ItemStack(CreatePort.FLOUR)); grid.setInventorySlotContents(1, new ItemStack(Items.WATER_BUCKET));
        if (CraftingManager.findMatchingResult(grid, w).getItem() != CreatePort.DOUGH) throw new AssertionError("Dough crafting recipe");
        if (CraftingManager.getRemainingItems(grid, w).get(1).getItem() != Items.BUCKET) throw new AssertionError("Water bucket container not returned");
        if (FurnaceRecipes.instance().getSmeltingResult(new ItemStack(CreatePort.DOUGH)).getItem() != Items.BREAD) throw new AssertionError("Dough baking");
        result.put("alloy_dough_bucket_and_bread_recipes", true);
        EntityPlayerMP player = mc.getIntegratedServer().getPlayerList().getPlayers().get(0);
        ItemStack previousHeld = player.getHeldItem(EnumHand.MAIN_HAND).copy();
        boolean wasCreative = player.capabilities.isCreativeMode;
        BlockPos casing = new BlockPos(31, 65, 5);
        w.setBlockState(casing, Blocks.LOG.getDefaultState());
        try {
            player.capabilities.isCreativeMode = false;
            player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(CreatePort.ANDESITE_ALLOY));
            if (CreatePort.ANDESITE_ALLOY.onItemUse(player, w, casing, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5f, 1, 0.5f) != net.minecraft.util.EnumActionResult.SUCCESS)
                throw new AssertionError("Alloy casing interaction failed");
            if (w.getBlockState(casing).getBlock() != CreatePort.ANDESITE_CASING || !player.getHeldItem(EnumHand.MAIN_HAND).isEmpty())
                throw new AssertionError("Casing conversion or survival item cost failed");
        } finally { player.capabilities.isCreativeMode = wasCreative; player.setHeldItem(EnumHand.MAIN_HAND, previousHeld); }
        result.put("survival_alloy_casing_conversion", true);
        result.put("loaded_milling_recipes", MillingRecipes.count());
    }
    private static void verify(Minecraft mc) {
        WorldServer w = mc.getIntegratedServer().getWorld(0);
        KineticTile crank = tile(w, CRANK), mill = tile(w, MILL), overload = tile(w, OVERLOAD);
        expect(crank.getCapacity(), 256, "Crank capacity"); expect(mill.getDemand(), 128, "Mill stress demand"); expect(mill.getSpeed(), 32, "Powered mill speed");
        expect(overload.getCapacity(), 256, "Overloaded capacity"); expect(overload.getDemand(), 512, "Geared-up stress");
        expect(overload.getSpeed(), 0, "Overload stalls mill");
        if (!overload.isOverloaded()) throw new AssertionError("Overload flag missing");
        IItemHandler output = mill.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN);
        ItemStack flour = output.getStackInSlot(0);
        if (flour.isEmpty() || flour.getItem() != CreatePort.FLOUR) throw new AssertionError("Powered wheat milling did not produce flour");
        IItemHandler blocked = overload.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN);
        if (!blocked.getStackInSlot(0).isEmpty()) throw new AssertionError("Overloaded mill processed an item");
        MillstoneTile restored = new MillstoneTile(); restored.readFromNBT(mill.writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
        if (restored.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN).getStackInSlot(0).getItem() != CreatePort.FLOUR) throw new AssertionError("Milling inventory persistence");
        if (output.extractItem(0, 64, false).getItem() != CreatePort.FLOUR) throw new AssertionError("Hopper-style extraction");
        result.put("milling_inventory_processing_and_persistence", true);
        result.put("stress_overload_and_stalling", true);
        result.put("capacity_su", crank.getCapacity()); result.put("normal_demand_su", mill.getDemand()); result.put("overload_demand_su", overload.getDemand());
        w.setBlockToAir(OVERLOAD);
        KineticTile overloadedSource = tile(w, new BlockPos(16, 65, 0)); overloadedSource.refreshNetwork();
        expect(overloadedSource.getSpeed(), 32, "Removing load recovers power");
        expect(overloadedSource.getDemand(), 0, "Removed load no longer consumes stress");
        place(w, CreatePort.MILLSTONE, 18, 69, 2, EnumFacing.Axis.Y); overloadedSource.refreshNetwork();
        if (!tile(w, OVERLOAD).isOverloaded()) throw new AssertionError("Restoring excessive load must stall again");
        result.put("overload_recovery_after_removing_load", true);
        mc.getIntegratedServer().getPlayerList().getPlayers().forEach(player -> player.connection.setPlayerLocation(25, 75, 18, 145, 25));
    }
}
