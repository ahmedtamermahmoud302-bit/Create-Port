package dev.createport.kinetics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** World-independent rotational constraints. A conflicting component stops safely. */
public final class KineticGraph<K> {
    private static final double EPSILON = 1e-6;
    private final Map<K, List<Edge<K>>> edges = new LinkedHashMap<>();
    private final Map<K, Double> sources = new LinkedHashMap<>();

    public void addNode(K key) { edges.computeIfAbsent(key, k -> new ArrayList<>()); }

    public void addSource(K key, double rpm) {
        if (!Double.isFinite(rpm)) throw new IllegalArgumentException("Non-finite source speed");
        addNode(key);
        sources.put(key, rpm);
    }

    /** target speed = source speed * ratio; the reverse constraint is implicit. */
    public void connect(K from, K to, double ratio) {
        if (!Double.isFinite(ratio) || ratio == 0) throw new IllegalArgumentException("Invalid ratio");
        addNode(from);
        addNode(to);
        edges.get(from).add(new Edge<>(to, ratio));
        edges.get(to).add(new Edge<>(from, 1 / ratio));
    }

    public Map<K, Double> solve() {
        Map<K, Double> result = new LinkedHashMap<>();
        for (K root : edges.keySet()) {
            if (result.containsKey(root)) continue;
            Map<K, Double> relative = new LinkedHashMap<>();
            ArrayDeque<K> pending = new ArrayDeque<>();
            relative.put(root, 1.0);
            pending.add(root);
            boolean conflict = false;
            while (!pending.isEmpty()) {
                K key = pending.remove();
                for (Edge<K> edge : edges.get(key)) {
                    double expected = relative.get(key) * edge.ratio;
                    if (!Double.isFinite(expected) || expected == 0) { conflict = true; continue; }
                    Double previous = relative.get(edge.to);
                    if (previous == null) {
                        relative.put(edge.to, expected);
                        pending.add(edge.to);
                    } else if (!near(previous, expected)) conflict = true;
                }
            }
            Double rootSpeed = null;
            for (Map.Entry<K, Double> entry : relative.entrySet()) {
                Double source = sources.get(entry.getKey());
                if (source == null) continue;
                double required = source / entry.getValue();
                if (!Double.isFinite(required)) conflict = true;
                if (rootSpeed == null) rootSpeed = required;
                else if (!near(rootSpeed, required)) conflict = true;
            }
            double speed = conflict || rootSpeed == null ? 0 : rootSpeed;
            for (double factor : relative.values()) {
                if (!Double.isFinite(factor * speed)) { speed = 0; break; }
            }
            for (Map.Entry<K, Double> entry : relative.entrySet())
                result.put(entry.getKey(), speed * entry.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) <= EPSILON * Math.max(1, Math.max(Math.abs(a), Math.abs(b)));
    }

    private static final class Edge<K> {
        final K to;
        final double ratio;
        Edge(K to, double ratio) { this.to = to; this.ratio = ratio; }
    }
}
