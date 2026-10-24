package dev.createport.kinetics;

import static dev.createport.kinetics.RotationRules.Axis.*;
import static dev.createport.kinetics.RotationRules.Kind.*;

public final class RotationRulesTest {
    private static int checks;
    public static void main(String[] args) {
        expect(RotationRules.ratio(MOTOR, X, SHAFT, X, 1, 0, 0), 1);
        expect(RotationRules.ratio(SHAFT, Y, SHAFT, Y, 0, -1, 0), 1);
        expect(RotationRules.ratio(SHAFT, Z, SMALL, Z, 0, 0, 1), 1);
        expect(RotationRules.ratio(SHAFT, X, SHAFT, Y, 1, 0, 0), 0);
        expect(RotationRules.ratio(SHAFT, X, SHAFT, X, 0, 1, 0), 0);
        expect(RotationRules.ratio(SMALL, Y, SMALL, Y, 1, 0, 0), -1);
        expect(RotationRules.ratio(SMALL, Y, SMALL, Y, 1, 0, 1), 0);
        expect(RotationRules.ratio(LARGE, Y, SMALL, Y, 1, 0, 1), -2);
        expect(RotationRules.ratio(SMALL, Y, LARGE, Y, -1, 0, -1), -0.5);
        expect(RotationRules.ratio(LARGE, X, SMALL, X, 0, 1, 1), -2);
        expect(RotationRules.ratio(LARGE, Z, SMALL, Z, 1, 1, 0), -2);
        expect(RotationRules.ratio(LARGE, Y, SMALL, Y, 1, 0, 0), 0);
        expect(RotationRules.ratio(LARGE, X, LARGE, Y, 1, 1, 0), 1);
        expect(RotationRules.ratio(LARGE, X, LARGE, Y, 1, -1, 0), -1);
        expect(RotationRules.ratio(LARGE, Y, LARGE, Y, 1, 0, 1), 0);
        expect(RotationRules.ratio(LARGE, Y, SMALL, Y, 2, 0, 0), 0);
        expect(RotationRules.ratio(MOTOR, X, SMALL, X, 0, 1, 0), 0);
        // Every supported nonzero local connection must be invertible from its other end.
        for (RotationRules.Kind from : RotationRules.Kind.values())
            for (RotationRules.Kind to : RotationRules.Kind.values())
                for (RotationRules.Axis a : RotationRules.Axis.values())
                    for (RotationRules.Axis b : RotationRules.Axis.values())
                        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                            double ratio = RotationRules.ratio(from, a, to, b, x, y, z);
                            if (ratio != 0) expect(ratio * RotationRules.ratio(to, b, from, a, -x, -y, -z), 1);
                        }
        System.out.println("Passed " + checks + " placement and reciprocal connection checks");
    }
    private static void expect(double actual, double expected) {
        if (Math.abs(actual - expected) > 0.000001) throw new AssertionError(actual + " != " + expected);
        checks++;
    }
}
