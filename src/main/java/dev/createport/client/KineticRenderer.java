package dev.createport.client;

import dev.createport.block.KineticBlock;
import dev.createport.block.KineticTile;
import dev.createport.block.DirectionalGeneratorBlock;
import dev.createport.kinetics.RotationRules.Kind;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;

/** Legacy TESR baseline: real Create geometry rotates around the selected shaft axis. */
public final class KineticRenderer extends TileEntitySpecialRenderer<KineticTile> {
    @Override public void render(KineticTile tile, double x, double y, double z, float partialTicks,
            int destroyStage, float alpha) {
        IBlockState state = tile.getWorld().getBlockState(tile.getPos());
        if (!(state.getBlock() instanceof KineticBlock)) return;
        Kind kind = ((KineticBlock) state.getBlock()).kind;
        EnumFacing.Axis axis = state.getValue(KineticBlock.AXIS);
        int light = tile.getWorld().getCombinedLight(tile.getPos(), 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 65535, light >> 16);
        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        if (kind == Kind.GEARBOX || kind == Kind.CLUTCH || kind == Kind.GEARSHIFT) {
            for (EnumFacing face : EnumFacing.values()) if (((KineticBlock) state.getBlock()).hasShaft(state, face))
                draw("shaft_half", tile.getPortAngle(face, partialTicks), face.getAxis(), face, false, x, y, z);
            return;
        }
        if (kind == Kind.CRANK) {
            EnumFacing face = state.getValue(DirectionalGeneratorBlock.FACING);
            draw("hand_crank/handle", tile.getRenderAngle(partialTicks), axis, face, false, x, y, z);
            draw("shaft_half", tile.getPortAngle(face.getOpposite(), partialTicks), axis, face.getOpposite(), false, x, y, z);
            return;
        }
        if (kind == Kind.WATER_WHEEL) {
            draw("water_wheel/wheel", tile.getRenderAngle(partialTicks), axis, state.getValue(DirectionalGeneratorBlock.FACING), true, x, y, z);
            return;
        }
        if (kind == Kind.MILLSTONE) {
            draw("millstone/inner", tile.getRenderAngle(partialTicks), axis, EnumFacing.UP, true, x, y, z);
            draw("shaft_half", tile.getPortAngle(EnumFacing.DOWN, partialTicks), axis, EnumFacing.DOWN, false, x, y, z);
            return;
        }
        String path = kind == Kind.SMALL ? "cogwheel" : kind == Kind.LARGE ? "large_cogwheel" : "shaft";
        double angle = tile.getRenderAngle(partialTicks);
        if (kind == Kind.SMALL || kind == Kind.LARGE) {
            int parity = (axis == EnumFacing.Axis.X ? tile.getPos().getY() + tile.getPos().getZ()
                : axis == EnumFacing.Axis.Y ? tile.getPos().getX() + tile.getPos().getZ()
                : tile.getPos().getX() + tile.getPos().getY()) & 1;
            angle += (kind == Kind.LARGE ? 11.25 : 0) + (parity == 0 ? 22.5 : 0);
        }
        draw(path, angle, axis, EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.POSITIVE, axis), true, x, y, z);
    }

    private void draw(String path, double angle, EnumFacing.Axis axis, EnumFacing facing, boolean verticalModel, double x, double y, double z) {
        IBakedModel model = ModelCatalog.INSTANCE.get(path);
        if (model == null) return;
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
            GlStateManager.rotate((float) angle, axis == EnumFacing.Axis.X ? 1 : 0,
                axis == EnumFacing.Axis.Y ? 1 : 0, axis == EnumFacing.Axis.Z ? 1 : 0);
            if (verticalModel) {
                if (facing == EnumFacing.DOWN) GlStateManager.rotate(180, 0, 0, 1);
                if (facing == EnumFacing.EAST) GlStateManager.rotate(-90, 0, 0, 1);
                if (facing == EnumFacing.WEST) GlStateManager.rotate(90, 0, 0, 1);
                if (facing == EnumFacing.SOUTH) GlStateManager.rotate(90, 1, 0, 0);
                if (facing == EnumFacing.NORTH) GlStateManager.rotate(-90, 1, 0, 0);
            } else {
                if (facing == EnumFacing.UP) GlStateManager.rotate(-90, 1, 0, 0);
                if (facing == EnumFacing.DOWN) GlStateManager.rotate(90, 1, 0, 0);
                if (facing == EnumFacing.EAST) GlStateManager.rotate(90, 0, 1, 0);
                if (facing == EnumFacing.WEST) GlStateManager.rotate(-90, 0, 1, 0);
                if (facing == EnumFacing.NORTH) GlStateManager.rotate(180, 0, 1, 0);
            }
            GlStateManager.translate(-0.5, -0.5, -0.5);
            GlStateManager.disableLighting();
            Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer()
                .renderModelBrightnessColor(model, 1, 1, 1, 1);
        } finally {
            GlStateManager.enableLighting();
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.popMatrix();
        }
    }
}
