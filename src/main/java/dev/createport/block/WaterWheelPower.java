package dev.createport.block;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

final class WaterWheelPower {
    static double speed(World world, BlockPos center, EnumFacing.Axis axis) {
        double score = 0;
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && y == 0 && z == 0) continue;
            if ((axis == EnumFacing.Axis.X ? x : axis == EnumFacing.Axis.Y ? y : z) != 0) continue;
            BlockPos pos = center.add(x, y, z);
            if (!world.isBlockLoaded(pos)) continue;
            IBlockState fluid = world.getBlockState(pos);
            if (!(fluid.getBlock() instanceof BlockLiquid)) continue;
            // Vanilla BlockLiquid adds its normalized flow and does not inspect the entity.
            Vec3d flow = fluid.getBlock().modifyAcceleration(world, pos, null, Vec3d.ZERO);
            Vec3d torque = new Vec3d(x, y, z).crossProduct(flow);
            double value = axis == EnumFacing.Axis.X ? torque.x : axis == EnumFacing.Axis.Y ? torque.y : torque.z;
            if (Math.abs(value) > 1e-6) score += Math.signum(value);
        }
        return Math.signum(score) * 8;
    }
}
