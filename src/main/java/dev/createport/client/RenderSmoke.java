package dev.createport.client;

import com.google.gson.GsonBuilder;
import dev.createport.CreatePort;
import dev.createport.block.KineticBlock;
import dev.createport.block.KineticTile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

/** Opt-in development smoke test; never runs in normal gameplay. */
@Mod.EventBusSubscriber(modid = CreatePort.ID, value = Side.CLIENT)
public final class RenderSmoke {
    private static final boolean ENABLED = Boolean.getBoolean("createport.renderSmoke");
    private static final boolean AUDIT_ONLY = Boolean.getBoolean("createport.renderAudit");
    private static final Map<String, Object> results = new LinkedHashMap<>();
    private static int stage, ticks, elapsed;
    private static String screenshot;
    private static boolean finish;
    private static double angle;

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if ((!ENABLED && !AUDIT_ONLY) || event.phase != TickEvent.Phase.END || finish) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (stage == 0 && mc.currentScreen instanceof GuiMainMenu) {
                if (AUDIT_ONLY) { finish = true; mc.shutdown(); return; }
                mc.gameSettings.pauseOnLostFocus = false;
                mc.gameSettings.renderDistanceChunks = 4;
                mc.gameSettings.hideGUI = true;
                mc.launchIntegratedServer("createport-render-smoke", "Create Port Render Smoke",
                    new WorldSettings(0, GameType.CREATIVE, false, false, WorldType.FLAT)
                        .setGeneratorOptions("3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;"));
                stage = 1;
                return;
            }
            if (stage == 0) return;
            if (++elapsed > 2400) throw new IllegalStateException("Render smoke timed out");
            if (mc.world == null || mc.player == null || mc.getIntegratedServer() == null) return;
            if (stage == 1) {
                mc.getIntegratedServer().addScheduledTask(() -> {
                    WorldServer world = mc.getIntegratedServer().getWorld(0);
                    world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
                    world.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
                    world.setWorldTime(6000);
                    for (int x = -3; x <= 7; x++) for (int z = -2; z <= 3; z++)
                        world.setBlockState(new BlockPos(x, 64, z), Blocks.STONEBRICK.getDefaultState());
                    for (int x = -3; x <= 7; x++) for (int y = 65; y <= 69; y++) for (int z = -2; z <= 3; z++)
                        world.setBlockToAir(new BlockPos(x, y, z));
                    world.setBlockState(new BlockPos(0, 65, 0), CreatePort.MOTOR.getDefaultState().withProperty(KineticBlock.AXIS, EnumFacing.Axis.X));
                    world.setBlockState(new BlockPos(1, 65, 0), CreatePort.SHAFT.getDefaultState().withProperty(KineticBlock.AXIS, EnumFacing.Axis.X));
                    world.setBlockState(new BlockPos(2, 65, 0), CreatePort.COG.getDefaultState().withProperty(KineticBlock.AXIS, EnumFacing.Axis.X));
                    world.setBlockState(new BlockPos(2, 66, 0), CreatePort.COG.getDefaultState().withProperty(KineticBlock.AXIS, EnumFacing.Axis.X));
                    world.setBlockState(new BlockPos(2, 67, 1), CreatePort.LARGE_COG.getDefaultState().withProperty(KineticBlock.AXIS, EnumFacing.Axis.X));
                    for (int y = 65; y <= 67; y++)
                        world.setBlockState(new BlockPos(5, y, 0), (y == 65 ? CreatePort.MOTOR : y == 66 ? CreatePort.SHAFT : CreatePort.COG)
                            .getDefaultState().withProperty(KineticBlock.AXIS, EnumFacing.Axis.Y));
                    mc.getIntegratedServer().getPlayerList().getPlayers().forEach(player -> {
                        player.capabilities.allowFlying = true;
                        player.capabilities.isFlying = true;
                        player.connection.setPlayerLocation(7, 69, 8, 145, 25);
                    });
                }).get();
                stage = 2; ticks = 0;
            }
            mc.player.capabilities.allowFlying = true;
            mc.player.capabilities.isFlying = true;
            mc.player.motionX = mc.player.motionY = mc.player.motionZ = 0;
            mc.player.rotationYaw = 145; mc.player.rotationPitch = 25;
            if (++ticks < 100 || screenshot != null) return;
            if (stage == 2) {
                check(mc, "powered", 16, -16, 8);
                angle = ((KineticTile) mc.world.getTileEntity(new BlockPos(1, 65, 0))).getRenderAngle(0);
                screenshot = "createport-powered.png";
                stage = 3; ticks = 0;
            } else if (stage == 3) {
                double next = ((KineticTile) mc.world.getTileEntity(new BlockPos(1, 65, 0))).getRenderAngle(0);
                if (Math.abs(next - angle) < 1) throw new AssertionError("Powered shaft did not advance its animation");
                results.put("powered_animation_advanced", true);
                mc.getIntegratedServer().addScheduledTask(() -> mc.getIntegratedServer().getWorld(0).setBlockToAir(new BlockPos(0, 65, 0))).get();
                stage = 4; ticks = 0;
            } else if (stage == 4) {
                check(mc, "removed_source", 0, 0, 0);
                angle = ((KineticTile) mc.world.getTileEntity(new BlockPos(1, 65, 0))).getRenderAngle(0);
                screenshot = "createport-stopped.png";
                stage = 5; ticks = 0;
            } else if (stage == 5) {
                double next = ((KineticTile) mc.world.getTileEntity(new BlockPos(1, 65, 0))).getRenderAngle(0);
                if (Math.abs(next - angle) > 0.001) throw new AssertionError("Unpowered shaft kept animating");
                results.put("unpowered_animation_stopped", true);
                mc.getIntegratedServer().addScheduledTask(() -> {
                    WorldServer world = mc.getIntegratedServer().getWorld(0);
                    world.setBlockState(new BlockPos(0, 65, 0), CreatePort.MOTOR.getDefaultState().withProperty(KineticBlock.AXIS, EnumFacing.Axis.X));
                    ((KineticTile) world.getTileEntity(new BlockPos(0, 65, 0))).reverse();
                }).get();
                stage = 6; ticks = 0;
            } else if (stage == 6) {
                check(mc, "reversed", -16, 16, -8);
                screenshot = "createport-reversed.png";
                stage = 7; ticks = 0;
            } else if (stage == 7) {
                results.put("passed", true);
                finish = true;
                save();
                mc.loadWorld(null);
                mc.shutdown();
            }
        } catch (Throwable e) {
            results.put("passed", false); results.put("error", e.toString());
            finish = true; save(); mc.shutdown();
        }
    }

    private static void check(Minecraft mc, String label, double shaft, double small, double large) throws Exception {
        BlockPos[] positions = {new BlockPos(1, 65, 0), new BlockPos(2, 66, 0), new BlockPos(2, 67, 1)};
        double[] expected = {shaft, small, large};
        for (int i = 0; i < positions.length; i++) {
            double actual = ((KineticTile) mc.world.getTileEntity(positions[i])).getSpeed();
            if (Math.abs(actual - expected[i]) > 0.001) throw new AssertionError(label + " client speed: " + actual + " expected " + expected[i]);
        }
        mc.getIntegratedServer().addScheduledTask(() -> {
            for (int i = 0; i < positions.length; i++) {
                double actual = ((KineticTile) mc.getIntegratedServer().getWorld(0).getTileEntity(positions[i])).getSpeed();
                if (Math.abs(actual - expected[i]) > 0.001) throw new AssertionError(label + " server speed: " + actual);
            }
        }).get();
        results.put(label + "_server_and_client_speeds", true);
    }

    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END || screenshot == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            int error = GL11.glGetError();
            if (error != GL11.GL_NO_ERROR) throw new IllegalStateException("OpenGL error " + error);
            ScreenShotHelper.saveScreenshot(mc.mcDataDir, screenshot, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            if (!new java.io.File(mc.mcDataDir, "screenshots/" + screenshot).isFile())
                throw new IllegalStateException("Screenshot was not written: " + screenshot);
            results.put(screenshot, true);
            screenshot = null;
        } catch (Throwable e) {
            results.put("passed", false); results.put("error", e.toString()); finish = true; save(); mc.shutdown();
        }
    }

    private static void save() {
        try { Files.write(Paths.get("render-smoke.json"), new GsonBuilder().setPrettyPrinting().create().toJson(results).getBytes(StandardCharsets.UTF_8)); }
        catch (Exception e) { throw new IllegalStateException("Cannot write render smoke result", e); }
    }
}
