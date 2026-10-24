package dev.createport.kinetics;

public final class MillingSpeed {
    private MillingSpeed() {}
    public static int workPerTick(double rpm) {
        if (!Double.isFinite(rpm) || rpm == 0) return 0;
        return Math.max(1, Math.min(512, (int) Math.abs(rpm / 16)));
    }
}
