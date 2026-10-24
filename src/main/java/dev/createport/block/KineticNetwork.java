package dev.createport.block;

import dev.createport.kinetics.KineticGraph;
import dev.createport.kinetics.RotationRules;
import dev.createport.kinetics.RotationRules.Kind;
import dev.createport.kinetics.TransmissionRules;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Each shaft face is an independent port, allowing disconnects and direction changes. */
final class KineticNetwork {
    static void solve(KineticTile start) {
        World world = start.getWorld();
        Map<BlockPos, KineticTile> tiles = new LinkedHashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        KineticGraph<Node> graph = new KineticGraph<>();
        Map<Node, Double> capacities = new LinkedHashMap<>(), impacts = new LinkedHashMap<>();
        tiles.put(start.getPos(), start); queue.add(start.getPos());
        boolean tooLarge = false;
        while (!queue.isEmpty()) {
            BlockPos from = queue.remove();
            IBlockState a = world.getBlockState(from);
            if (!(a.getBlock() instanceof KineticBlock)) continue;
            KineticBlock ab = (KineticBlock) a.getBlock();
            KineticTile tile = tiles.get(from);
            Node center = new Node(from, null); graph.addNode(center);
            double generated = tile.getGeneratedSpeed();
            if (generated != 0) {
                graph.addSource(center, generated);
                capacities.put(center, ab.kind == Kind.MOTOR ? 16384.0 : ab.kind == Kind.CRANK ? 8.0 : 32.0);
            }
            impacts.put(center, tile.getStressImpact());
            for (EnumFacing face : EnumFacing.values()) {
                if (!ab.hasShaft(a, face)) continue;
                Node port = new Node(from, face); graph.addNode(port);
                double factor = 1;
                if (ab.kind == Kind.GEARBOX)
                    factor = TransmissionRules.gearboxFactor(axis(a.getValue(KineticBlock.AXIS)), axis(face.getAxis()), face.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE);
                if (ab.kind == Kind.CLUTCH || ab.kind == Kind.GEARSHIFT)
                    factor = TransmissionRules.splitFactor(ab.kind == Kind.CLUTCH, a.getValue(TransmissionBlock.POWERED), face.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE);
                if (factor != 0) graph.connect(center, port, factor);
            }
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                int distance = Math.abs(x) + Math.abs(y) + Math.abs(z);
                if (distance == 0) continue;
                BlockPos to = from.add(x, y, z);
                if (!world.isBlockLoaded(to)) continue;
                IBlockState b = world.getBlockState(to);
                if (!(b.getBlock() instanceof KineticBlock)) continue;
                KineticBlock bb = (KineticBlock) b.getBlock();
                EnumFacing face = EnumFacing.getFacingFromVector(x, y, z);
                boolean axle = distance == 1 && ab.hasShaft(a, face) && bb.hasShaft(b, face.getOpposite());
                boolean gears = (ab.kind == Kind.SMALL || ab.kind == Kind.LARGE) && (bb.kind == Kind.SMALL || bb.kind == Kind.LARGE);
                double ratio = axle ? 1 : gears ? RotationRules.ratio(ab.kind, axis(a.getValue(KineticBlock.AXIS)), bb.kind, axis(b.getValue(KineticBlock.AXIS)), x, y, z) : 0;
                if (ratio == 0) continue;
                TileEntity candidate = world.getTileEntity(to);
                if (!(candidate instanceof KineticTile)) continue;
                if (!tiles.containsKey(to)) {
                    if (tiles.size() >= 512) { tooLarge = true; continue; }
                    tiles.put(to, (KineticTile) candidate); queue.add(to);
                }
                graph.connect(new Node(from, axle ? face : null), new Node(to, axle ? face.getOpposite() : null), ratio);
            }
        }
        KineticGraph.Result<Node> result = graph.solveWithStress(capacities, impacts);
        for (Map.Entry<BlockPos, KineticTile> entry : tiles.entrySet()) {
            Node center = new Node(entry.getKey(), null);
            double[] ports = new double[6];
            for (EnumFacing face : EnumFacing.values()) ports[face.getIndex()] = tooLarge ? 0 : result.speeds.getOrDefault(new Node(entry.getKey(), face), 0.0);
            entry.getValue().applyNetwork(tooLarge ? 0 : result.speeds.getOrDefault(center, 0.0), ports, result.energy.get(center));
        }
    }
    private static RotationRules.Axis axis(EnumFacing.Axis value) { return RotationRules.Axis.valueOf(value.name()); }
    private static final class Node {
        final BlockPos pos; final EnumFacing face;
        Node(BlockPos pos, EnumFacing face) { this.pos = pos; this.face = face; }
        @Override public int hashCode() { return Objects.hash(pos, face); }
        @Override public boolean equals(Object other) { return other instanceof Node && pos.equals(((Node) other).pos) && face == ((Node) other).face; }
    }
}
