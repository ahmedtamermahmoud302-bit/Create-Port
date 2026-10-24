package dev.createport.block;

import dev.createport.kinetics.RotationRules.Kind;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class TransmissionBlock extends KineticBlock {
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    public TransmissionBlock(String name, Kind kind) { super(name, kind); }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, AXIS, POWERED); }
    @Override public int getMetaFromState(IBlockState state) { return super.getMetaFromState(state) | (state.getValue(POWERED) ? 4 : 0); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(AXIS, EnumFacing.Axis.values()[Math.floorMod(meta & 3, 3)]).withProperty(POWERED, (meta & 4) != 0); }
    @Override public void onBlockAdded(World world, BlockPos pos, IBlockState state) { update(world, pos, state); }
    @Override public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) { update(world, pos, state); }
    private void update(World world, BlockPos pos, IBlockState state) {
        if (world.isRemote) return;
        boolean powered = world.isBlockPowered(pos);
        if (state.getValue(POWERED) != powered) {
            world.setBlockState(pos, state.withProperty(POWERED, powered), 3);
            if (world.getTileEntity(pos) instanceof KineticTile) ((KineticTile) world.getTileEntity(pos)).refreshNetwork();
        }
    }
}
