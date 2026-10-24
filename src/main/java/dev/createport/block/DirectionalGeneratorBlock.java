package dev.createport.block;

import dev.createport.kinetics.RotationRules.Kind;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class DirectionalGeneratorBlock extends KineticBlock {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    public DirectionalGeneratorBlock(String name, Kind kind) { super(name, kind); setDefaultState(getDefaultState().withProperty(FACING, EnumFacing.UP)); }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, AXIS, FACING); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getIndex(); }
    @Override public IBlockState getStateFromMeta(int meta) {
        EnumFacing face = EnumFacing.getFront(meta);
        return getDefaultState().withProperty(FACING, face).withProperty(AXIS, face.getAxis());
    }
    @Override public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing face, float x, float y, float z, int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, face).withProperty(AXIS, face.getAxis());
    }
}
