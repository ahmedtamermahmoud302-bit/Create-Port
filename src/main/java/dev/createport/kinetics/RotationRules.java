package dev.createport.kinetics;

/** Geometric connections for the prototype's four block types. */
public final class RotationRules {
    public enum Kind { MOTOR, SHAFT, SMALL, LARGE }
    public enum Axis { X, Y, Z }
    private RotationRules() {}

    public static double ratio(Kind from, Axis a, Kind to, Axis b, int x, int y, int z) {
        // Only the local 3x3x3 neighbourhood can connect these blocks.
        if (Math.abs(x) > 1 || Math.abs(y) > 1 || Math.abs(z) > 1) return 0;
        int distance = Math.abs(x) + Math.abs(y) + Math.abs(z);
        int alongA = component(a, x, y, z), alongB = component(b, x, y, z);
        if (a == b && distance == 1 && Math.abs(alongA) == 1) return 1;
        if (a == b && alongA == 0) {
            if (from == Kind.SMALL && to == Kind.SMALL && distance == 1) return -1;
            if (distance == 2 && from == Kind.LARGE && to == Kind.SMALL) return -2;
            if (distance == 2 && from == Kind.SMALL && to == Kind.LARGE) return -0.5;
        }
        if (from == Kind.LARGE && to == Kind.LARGE && a != b && distance == 2
                && Math.abs(alongA) == 1 && Math.abs(alongB) == 1)
            return (alongA > 0) != (alongB > 0) ? -1 : 1;
        return 0;
    }

    private static int component(Axis axis, int x, int y, int z) {
        return axis == Axis.X ? x : axis == Axis.Y ? y : z;
    }
}
