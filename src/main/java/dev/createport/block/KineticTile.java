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
public final class KineticTile extends TileEntity implements ITickable {
    private static final int NETWORK_LIMIT = 512;
    private double speed;
    private double sourceSpeed = 16;
    private long solvedAt = Long.MIN_VALUE;
    private double renderAngle;
    private double previousRenderAngle;

    public double getSpeed() { return speed; }
    public double getRenderAngle(float partialTicks) {
        return previousRenderAngle + (renderAngle - previousRenderAngle) * partialTicks;
    }

    public void reverse() {
        sourceSpeed = -sourceSpeed;
        markDirty();
        solveNetwork();
    }

    @Override public void update() {
        if (world.isRemote) {
            previousRenderAngle = renderAngle;
            renderAngle += speed * 0.3;
            if (Math.abs(renderAngle) > 36000) {
                double turns = Math.floor(renderAngle / 360) * 360;
                renderAngle -= turns;
                previousRenderAngle -= turns;
            }
            return;
        }
        if (!world.isRemote && world.getTotalWorldTime() % 20 == 0 && solvedAt != world.getTotalWorldTime())
            solveNetwork();
    }

    private void solveNetwork() {
        if (world == null || world.isRemote) return;
        Map<BlockPos, KineticTile> nodes = new LinkedHashMap<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        KineticGraph<BlockPos> graph = new KineticGraph<>();
        nodes.put(pos, this);
        pending.add(pos);
        boolean tooLarge = false;
        while (!pending.isEmpty()) {
            BlockPos from = pending.remove();
            IBlockState a = world.getBlockState(from);
            if (!(a.getBlock() instanceof KineticBlock)) continue;
            graph.addNode(from);
            if (((KineticBlock) a.getBlock()).kind == Kind.MOTOR)
                graph.addSource(from, nodes.get(from).sourceSpeed);
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                if (x == 0 && y == 0 && z == 0) continue;
                BlockPos to = from.add(x, y, z);
                if (!world.isBlockLoaded(to)) continue;
                IBlockState b = world.getBlockState(to);
                if (!(b.getBlock() instanceof KineticBlock)) continue;
                double ratio = ratio(a, b, x, y, z);
                if (ratio == 0) continue;
                TileEntity candidate = world.getTileEntity(to);
                if (!(candidate instanceof KineticTile)) continue;
                if (!nodes.containsKey(to)) {
                    if (nodes.size() >= NETWORK_LIMIT) { tooLarge = true; continue; }
                    nodes.put(to, (KineticTile) candidate);
                    pending.add(to);
                }
                graph.connect(from, to, ratio);
            }
        }
        Map<BlockPos, Double> speeds = graph.solve();
        for (Map.Entry<BlockPos, KineticTile> entry : nodes.entrySet()) {
            KineticTile tile = entry.getValue();
            tile.solvedAt = world.getTotalWorldTime();
            double next = tooLarge ? 0 : speeds.getOrDefault(entry.getKey(), 0.0);
            if (Math.abs(tile.speed - next) < 0.000001) continue;
            tile.speed = next;
            tile.markDirty();
            IBlockState state = world.getBlockState(entry.getKey());
            world.notifyBlockUpdate(entry.getKey(), state, state, 3);
        }
    }

    private static double ratio(IBlockState a, IBlockState b, int x, int y, int z) {
        return RotationRules.ratio(((KineticBlock) a.getBlock()).kind,
            RotationRules.Axis.valueOf(a.getValue(KineticBlock.AXIS).name()),
            ((KineticBlock) b.getBlock()).kind,
            RotationRules.Axis.valueOf(b.getValue(KineticBlock.AXIS).name()), x, y, z);
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setDouble("Speed", speed);
        tag.setDouble("SourceSpeed", sourceSpeed);
        return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        // Derived network speed is recomputed after load. Clients receive it through update packets.
        speed = 0;
        sourceSpeed = tag.hasKey("SourceSpeed") && tag.getDouble("SourceSpeed") < 0 ? -16 : 16;
    }
    @Override public NBTTagCompound getUpdateTag() { return writeToNBT(new NBTTagCompound()); }
    @Override public SPacketUpdateTileEntity getUpdatePacket() { return new SPacketUpdateTileEntity(pos, 0, getUpdateTag()); }
    @Override public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
        double incoming = tag.getDouble("Speed");
        speed = Double.isFinite(incoming) ? incoming : 0;
    }
    @Override public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        handleUpdateTag(packet.getNbtCompound());
    }
}
