package dev.createport.block;

import dev.createport.CreatePort;
import net.minecraft.block.BlockLog;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class AndesiteAlloyItem extends Item {
    @Override public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
            EnumFacing face, float x, float y, float z) {
        if (!(world.getBlockState(pos).getBlock() instanceof BlockLog) || !player.canPlayerEdit(pos, face, player.getHeldItem(hand))) return EnumActionResult.PASS;
        if (!world.isRemote) {
            world.setBlockState(pos, CreatePort.ANDESITE_CASING.getDefaultState(), 3);
            if (!player.capabilities.isCreativeMode) player.getHeldItem(hand).shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }
}
