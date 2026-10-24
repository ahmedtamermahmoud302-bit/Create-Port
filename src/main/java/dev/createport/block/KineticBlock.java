package dev.createport.block;

import dev.createport.CreatePort;
import dev.createport.kinetics.RotationRules.Kind;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.init.Items;
import java.util.Locale;

public class KineticBlock extends Block {
    public static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis", EnumFacing.Axis.class);
    public final Kind kind;

    public KineticBlock(String name, Kind kind) {
        super(Material.IRON);
        this.kind = kind;
        setRegistryName(CreatePort.ID, name);
        setUnlocalizedName(CreatePort.ID + "." + name);
        setCreativeTab(CreativeTabs.REDSTONE);
        setHardness(2);
        setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Y));
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, AXIS); }
    @Override public int getMetaFromState(IBlockState state) { return kind == Kind.MILLSTONE ? 1 : state.getValue(AXIS).ordinal(); }
    @Override public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(AXIS, kind == Kind.MILLSTONE ? EnumFacing.Axis.Y : EnumFacing.Axis.values()[Math.floorMod(meta, 3)]);
    }
    @Override public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
            float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(AXIS, kind == Kind.MILLSTONE ? EnumFacing.Axis.Y : facing.getAxis());
    }
    public boolean hasShaft(IBlockState state, EnumFacing face) {
        if (kind == Kind.GEARBOX) return face.getAxis() != state.getValue(AXIS);
        if (kind == Kind.MILLSTONE) return face == EnumFacing.DOWN;
        if (kind == Kind.CRANK) return face == state.getValue(DirectionalGeneratorBlock.FACING).getOpposite();
        return face.getAxis() == state.getValue(AXIS);
    }
    @Override public boolean hasTileEntity(IBlockState state) { return true; }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public EnumBlockRenderType getRenderType(IBlockState state) {
        return kind == Kind.SHAFT || kind == Kind.SMALL || kind == Kind.LARGE || kind == Kind.WATER_WHEEL
            ? EnumBlockRenderType.ENTITYBLOCK_ANIMATED : EnumBlockRenderType.MODEL;
    }
    @Override public TileEntity createTileEntity(World world, IBlockState state) { return kind == Kind.MILLSTONE ? new MillstoneTile() : new KineticTile(); }

    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!world.isRemote && hand == EnumHand.MAIN_HAND) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof KineticTile) {
                KineticTile kinetic = (KineticTile) tile;
                if (tile instanceof MillstoneTile) return ((MillstoneTile) tile).interact(player, hand);
                if (kind == Kind.CRANK) kinetic.crank(player.isSneaking());
                if (kind == Kind.MOTOR) {
                    if (player.isSneaking()) kinetic.reverse();
                    else if (player.getHeldItem(hand).getItem() == Items.REDSTONE) kinetic.adjustMotor();
                }
                player.sendStatusMessage(new TextComponentString(String.format(Locale.ROOT,
                    "%sSpeed: %.0f RPM | Stress: %.0f / %.0f SU", kinetic.isOverloaded() ? "OVERSTRESSED | " : "",
                    kinetic.getSpeed(), kinetic.getDemand(), kinetic.getCapacity())), true);
            }
        }
        return true;
    }

    @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof MillstoneTile) ((MillstoneTile) tile).dropInventory();
        super.breakBlock(world, pos, state);
    }
}
