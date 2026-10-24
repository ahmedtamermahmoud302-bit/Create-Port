package dev.createport.block;

import dev.createport.CreatePort;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/** A usable milling inventory; hoppers insert at sides/top and extract below. */
public final class MillstoneTile extends KineticTile {
    private double progress;
    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override protected void onContentsChanged(int slot) { markDirty(); }
    };
    private final IItemHandler input = new Port(false), output = new Port(true);
    @Override public double getStressImpact() { return 4; }

    @Override public void update() {
        super.update();
        if (world.isRemote) return;
        MillingRecipes.Recipe recipe = MillingRecipes.find(inventory.getStackInSlot(0));
        if (recipe == null) { if (progress != 0) { progress = 0; markDirty(); } return; }
        if (!insertResults(recipe.outputs, true)) return;
        if (getSpeed() == 0 || isOverloaded()) return;
        progress += dev.createport.kinetics.MillingSpeed.workPerTick(getSpeed());
        if (progress >= recipe.time) {
            ItemStack remaining = inventory.getStackInSlot(0).copy(); remaining.shrink(1);
            inventory.setStackInSlot(0, remaining);
            insertResults(recipe.roll(world.rand), false);
            progress = 0;
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
        markDirty();
    }
    private boolean insertResults(java.util.List<ItemStack> results, boolean simulate) {
        ItemStack[] next = new ItemStack[3];
        for (int i = 0; i < 3; i++) next[i] = inventory.getStackInSlot(i + 1).copy();
        for (ItemStack result : results) {
            ItemStack remaining = result.copy();
            for (int i = 0; i < 3 && !remaining.isEmpty(); i++) {
                if (!next[i].isEmpty() && ItemHandlerHelper.canItemStacksStack(next[i], remaining)) {
                    int count = Math.min(remaining.getCount(), next[i].getMaxStackSize() - next[i].getCount());
                    next[i].grow(count); remaining.shrink(count);
                }
            }
            for (int i = 0; i < 3 && !remaining.isEmpty(); i++) if (next[i].isEmpty()) {
                next[i] = remaining.copy(); remaining = ItemStack.EMPTY;
            }
            if (!remaining.isEmpty()) return false;
        }
        if (!simulate) for (int i = 0; i < 3; i++) inventory.setStackInSlot(i + 1, next[i]);
        return true;
    }
    public boolean interact(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty()) {
            ItemStack remaining = input.insertItem(0, held, false);
            player.setHeldItem(hand, remaining);
        } else {
            boolean extractedAny = false;
            for (int i = 0; i < 3; i++) {
                ItemStack extracted = output.extractItem(i, 64, false);
                if (!extracted.isEmpty()) { ItemHandlerHelper.giveItemToPlayer(player, extracted); extractedAny = true; }
            }
            if (!extractedAny) player.sendStatusMessage(new TextComponentString(isOverloaded() ? "Millstone overstressed" : "Milling progress: " + (int) progress + " ticks | " + (int) getSpeed() + " RPM"), true);
        }
        return true;
    }
    public void dropInventory() {
        for (int i = 0; i < 4; i++) InventoryHelper.spawnItemStack(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, inventory.getStackInSlot(i));
    }
    @Override public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }
    @Override public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(facing == EnumFacing.DOWN ? output : input);
        return super.getCapability(capability, facing);
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag); tag.setTag("Inventory", inventory.serializeNBT()); tag.setDouble("MillingProgress", progress); return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag); inventory.deserializeNBT(tag.getCompoundTag("Inventory"));
        progress = Math.max(0, Math.min(1000, tag.getDouble("MillingProgress")));
    }
    private final class Port implements IItemHandler {
        private final boolean extracting;
        Port(boolean extracting) { this.extracting = extracting; }
        private int slot(int slot) { return extracting ? slot + 1 : 0; }
        private void check(int slot) { if (slot < 0 || slot >= getSlots()) throw new IndexOutOfBoundsException(); }
        @Override public int getSlots() { return extracting ? 3 : 1; }
        @Override public ItemStack getStackInSlot(int slot) { check(slot); return inventory.getStackInSlot(slot(slot)); }
        @Override public int getSlotLimit(int slot) { check(slot); return 64; }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            check(slot); return extracting || MillingRecipes.find(stack) == null ? stack : inventory.insertItem(0, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            check(slot); return extracting ? inventory.extractItem(slot(slot), amount, simulate) : ItemStack.EMPTY;
        }
    }
}
