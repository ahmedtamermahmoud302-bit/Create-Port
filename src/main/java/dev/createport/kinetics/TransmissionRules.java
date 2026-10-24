package dev.createport.kinetics;

import dev.createport.kinetics.RotationRules.Axis;

/** Port factors use a common world-axis sign convention. */
public final class TransmissionRules {
    private TransmissionRules() {}
    public static double gearboxFactor(Axis excluded, Axis port, boolean positive) {
        if (port == excluded) return 0;
        Axis first = excluded == Axis.X ? Axis.Y : Axis.X;
        return (port == first ? 1 : -1) * (positive ? 1 : -1);
    }
    public static double splitFactor(boolean clutch, boolean powered, boolean positive) {
        if (!positive) return 1;
        if (clutch && powered) return 0;
        return !clutch && powered ? -1 : 1;
    }
}
