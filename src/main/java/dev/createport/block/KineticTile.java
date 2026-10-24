package dev.createport.block;

import dev.createport.kinetics.KineticGraph;
import dev.createport.kinetics.RotationRules;
import dev.createport.kinetics.RotationRules.Kind;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

/** Small prototype networks are re-evaluated once per second, without loading chunks. */
public class KineticTile extends TileEntity implements ITickable {
    private static final int NETWORK_LIMIT = 512;
    private double speed;
    private double sourceSpeed = 16;
    private double capacity, demand;
    private boolean overloaded;
    private int crankTicks;
    private double crankSpeed;
    private final double[] portSpeeds = new double[6], portAngles = new double[6], previousPortAngles = new double[6];
    private long solvedAt = Long.MIN_VALUE;
    private double renderAngle;
    private double previousRenderAngle;

    public double getSpeed() { return speed; }
    @Override public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() { return new net.minecraft.util.math.AxisAlignedBB(pos).grow(2); }
    public double getRenderAngle(float partialTicks) {
        return previousRenderAngle + (renderAngle - previousRenderAngle) * partialTicks;
    }

    public double getCapacity() { return capacity; }
    public double getDemand() { return demand; }
    public boolean isOverloaded() { return overloaded; }
    public double getStressImpact() { return 0; }
    public double getPortSpeed(net.minecraft.util.EnumFacing face) { return portSpeeds[face.getIndex()]; }
    public double getPortAngle(net.minecraft.util.EnumFacing face, float partial) {
        int i = face.getIndex(); return previousPortAngles[i] + (portAngles[i] - previousPortAngles[i]) * partial;
    }
    public void refreshNetwork() { if (world != null && !world.isRemote) solveNetwork(); }
    public void crank(boolean backwards) { crankTicks = 10; crankSpeed = backwards ? -32 : 32; refreshNetwork(); }
    public void adjustMotor() {
        sourceSpeed = Math.copySign((Math.abs(sourceSpeed) + 16) % 272, sourceSpeed == 0 ? 1 : sourceSpeed);
        markDirty(); refreshNetwork();
    }
    public double getGeneratedSpeed() {
        net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof KineticBlock)) return 0;
        Kind kind = ((KineticBlock) state.getBlock()).kind;
        return kind == Kind.MOTOR ? sourceSpeed : kind == Kind.CRANK ? (crankTicks > 0 ? crankSpeed : 0)
            : kind == Kind.WATER_WHEEL ? WaterWheelPower.speed(world, pos, state.getValue(KineticBlock.AXIS)) : 0;
    }
    void applyNetwork(double next, double[] ports, KineticGraph.Energy energy) {
        double available = energy == null ? 0 : energy.capacity, needed = energy == null ? 0 : energy.demand;
        boolean stalled = energy != null && energy.overloaded;
        boolean changed = speed != next || capacity != available || demand != needed || overloaded != stalled || !java.util.Arrays.equals(portSpeeds, ports);
        speed = next; capacity = available; demand = needed; overloaded = stalled;
        System.arraycopy(ports, 0, portSpeeds, 0, 6); solvedAt = world.getTotalWorldTime();
        if (changed) { markDirty(); IBlockState state = world.getBlockState(pos); world.notifyBlockUpdate(pos, state, state, 3); }
    }

    public void reverse() {
        sourceSpeed = -sourceSpeed;
        markDirty();
        solveNetwork();
    }

    @Override public void update() {
        if (world.isRemote) {
            previousRenderAngle = renderAngle;
            renderAngle += (speed % 1200) * 0.3;
            for (int i = 0; i < 6; i++) {
                previousPortAngles[i] = portAngles[i];
                portAngles[i] += (portSpeeds[i] % 1200) * 0.3;
                if (Math.abs(portAngles[i]) > 36000) {
                    double turns = Math.floor(portAngles[i] / 360) * 360;
                    portAngles[i] -= turns; previousPortAngles[i] -= turns;
                }
            }
            if (Math.abs(renderAngle) > 36000) {
                double turns = Math.floor(renderAngle / 360) * 360;
                renderAngle -= turns;
                previousRenderAngle -= turns;
            }
            return;
        }
        if (crankTicks > 0 && --crankTicks == 0) refreshNetwork();
        if (!world.isRemote && world.getTotalWorldTime() % 20 == 0 && solvedAt != world.getTotalWorldTime())
            solveNetwork();
    }

    private void solveNetwork() { KineticNetwork.solve(this); }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setDouble("Speed", speed);
        tag.setDouble("SourceSpeed", sourceSpeed);
        tag.setDouble("Capacity", capacity); tag.setDouble("Demand", demand); tag.setBoolean("Overloaded", overloaded);
        for (int i = 0; i < 6; i++) tag.setDouble("Port" + i, portSpeeds[i]);
        return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        // Derived network speed is recomputed after load. Clients receive it through update packets.
        speed = 0;
        double configured = tag.hasKey("SourceSpeed") ? tag.getDouble("SourceSpeed") : 16;
        sourceSpeed = Double.isFinite(configured) ? Math.max(-256, Math.min(256, configured)) : 16;
        crankTicks = 0;
    }
    @Override public NBTTagCompound getUpdateTag() { return writeToNBT(new NBTTagCompound()); }
    @Override public SPacketUpdateTileEntity getUpdatePacket() { return new SPacketUpdateTileEntity(pos, 0, getUpdateTag()); }
    @Override public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
        double incoming = tag.getDouble("Speed");
        speed = Double.isFinite(incoming) ? incoming : 0;
        capacity = tag.getDouble("Capacity"); demand = tag.getDouble("Demand"); overloaded = tag.getBoolean("Overloaded");
        for (int i = 0; i < 6; i++) portSpeeds[i] = tag.getDouble("Port" + i);
    }
    @Override public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        handleUpdateTag(packet.getNbtCompound());
    }
}
