package dev.createport.kinetics;

import java.util.Map;

/** Dependency-free checks, runnable on the target Java 8 toolchain. */
public final class KineticGraphTest {
    private static int checks;
    public static void main(String[] args) {
        KineticGraph<String> g = new KineticGraph<>();
        g.addSource("motor", 16);
        g.connect("motor", "shaft", 1);
        g.connect("shaft", "small", -1);
        g.connect("small", "large", -0.5);
        Map<String, Double> speed = g.solve();
        expect(speed.get("shaft"), 16);
        expect(speed.get("small"), -16);
        expect(speed.get("large"), 8);

        KineticGraph<String> reverse = new KineticGraph<>();
        reverse.connect("small", "large", -0.5);
        reverse.addSource("large", 16);
        expect(reverse.solve().get("small"), -32);

        KineticGraph<String> disconnected = new KineticGraph<>();
        disconnected.addSource("motor", 32);
        disconnected.addNode("detached");
        expect(disconnected.solve().get("detached"), 0);

        KineticGraph<String> loop = new KineticGraph<>();
        loop.addSource("motor", 16);
        loop.connect("motor", "a", 1);
        loop.connect("a", "b", -1);
        loop.connect("b", "motor", 1);
        expect(loop.solve().get("motor"), 0);
        expect(loop.solve().get("b"), 0);

        KineticGraph<String> consistent = new KineticGraph<>();
        consistent.addSource("a", 16);
        consistent.addSource("b", -16);
        consistent.connect("a", "b", -1);
        expect(consistent.solve().get("b"), -16);
        consistent.addSource("b", 16);
        expect(consistent.solve().get("a"), 0);

        KineticGraph<Integer> longChain = new KineticGraph<>();
        longChain.addSource(0, 64);
        for (int i = 0; i < 10000; i++) longChain.connect(i, i + 1, 1);
        expect(longChain.solve().get(10000), 64);
        try { longChain.connect(0, 1, 0); throw new AssertionError("Accepted zero ratio"); }
        catch (IllegalArgumentException expected) { checks++; }
        try { longChain.addSource(0, Double.NaN); throw new AssertionError("Accepted NaN"); }
        catch (IllegalArgumentException expected) { checks++; }
        System.out.println("Passed " + checks + " kinetic constraint checks");
    }
    private static void expect(double actual, double expected) {
        if (Math.abs(actual - expected) > 0.000001) throw new AssertionError(actual + " != " + expected);
        checks++;
    }
}
