package dev.createport.kinetics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EnergyTest {
    private static int checks;
    public static void main(String[] args) {
        KineticGraph<String> g = new KineticGraph<>();
        g.addSource("crank", 32); g.connect("crank", "mill", 1);
        Map<String, Double> capacity = new LinkedHashMap<>(), impact = new LinkedHashMap<>();
        capacity.put("crank", 8.0); impact.put("mill", 4.0);
        KineticGraph.Result<String> result = g.solveWithStress(capacity, impact);
        expect(result.speeds.get("mill"), 32);
        expect(result.energy.get("mill").capacity, 256);
        expect(result.energy.get("mill").demand, 128);
        if (result.energy.get("mill").overloaded) throw new AssertionError("False overload");
        checks++;
        KineticGraph<String> fast = new KineticGraph<>();
        fast.addSource("crank", 32); fast.connect("crank", "mill", 4);
        result = fast.solveWithStress(capacity, impact);
        expect(result.speeds.get("mill"), 0);
        expect(result.speeds.get("crank"), 0);
        expect(result.energy.get("mill").demand, 512);
        if (!result.energy.get("mill").overloaded) throw new AssertionError("Overload not detected");
        checks++;
        capacity.put("crank", 16.0);
        expect(fast.solveWithStress(capacity, impact).speeds.get("mill"), 128);
        // A separate network must keep running when the first network stalls.
        capacity.put("crank", 8.0); fast.addSource("other", 16);
        expect(fast.solveWithStress(capacity, impact).speeds.get("other"), 16);
        // Power through a clutch is explicitly disconnected, not an artificial ratio of zero.
        KineticGraph<String> clutch = new KineticGraph<>();
        clutch.addSource("in", 16); clutch.addNode("out");
        expect(clutch.solve().get("out"), 0);
        expect(TransmissionRules.splitFactor(true, true, true), 0);
        expect(TransmissionRules.splitFactor(false, true, true), -1);
        expect(TransmissionRules.splitFactor(false, false, true), 1);
        expect(TransmissionRules.gearboxFactor(RotationRules.Axis.Y, RotationRules.Axis.Y, true), 0);
        expect(TransmissionRules.gearboxFactor(RotationRules.Axis.Y, RotationRules.Axis.X, true), 1);
        expect(TransmissionRules.gearboxFactor(RotationRules.Axis.Y, RotationRules.Axis.Z, true), -1);
        try { g.solveWithStress(Collections.singletonMap("crank", -1.0), impact); throw new AssertionError("Negative capacity accepted"); }
        catch (IllegalArgumentException expected) { checks++; }
        expect(MillingSpeed.workPerTick(0), 0);
        expect(MillingSpeed.workPerTick(8), 1);
        expect(MillingSpeed.workPerTick(-8), 1);
        expect(MillingSpeed.workPerTick(31), 1);
        expect(MillingSpeed.workPerTick(32), 2);
        expect(MillingSpeed.workPerTick(256), 16);
        expect(MillingSpeed.workPerTick(16384), 512);
        System.out.println("Passed " + checks + " energy and transmission checks");
    }
    private static void expect(double actual, double expected) {
        if (Math.abs(actual - expected) > 1e-6) throw new AssertionError(actual + " != " + expected);
        checks++;
    }
}
