package one.pkg.tinyutils.map;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenCustomHashMap;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * A Map-like data structure that uses weakly referenced Object keys and primitive double values.
 * Similar to {@link java.util.WeakHashMap}, an entry is automatically removed when its key
 * is no longer ordinarily reachable.
 *
 * @param <K> the type of keys maintained by this map
 */
public class WeakObject2DoubleHashMap<K> {
    private static final Strategy<Object> STRATEGY = new Strategy<>() {
        @Override
        public int hashCode(Object o) {
            if (o instanceof WeakKey) {
                return ((WeakKey<?>) o).hash;
            }
            return o.hashCode();
        }

        @Override
        public boolean equals(Object a, Object b) {
            if (a == b) return true;
            if (a == null || b == null) return false;
            Object k1 = a instanceof WeakKey ? ((WeakKey<?>) a).get() : a;
            Object k2 = b instanceof WeakKey ? ((WeakKey<?>) b).get() : b;
            if (k1 == null || k2 == null) return false;
            return k1.equals(k2);
        }
    };

    private final Object2DoubleOpenCustomHashMap<Object> map;
    private final ReferenceQueue<K> queue = new ReferenceQueue<>();

    public WeakObject2DoubleHashMap() {
        this.map = new Object2DoubleOpenCustomHashMap<>(STRATEGY);
    }

    public WeakObject2DoubleHashMap(int expected) {
        this.map = new Object2DoubleOpenCustomHashMap<>(expected, STRATEGY);
    }

    public WeakObject2DoubleHashMap(int expected, float f) {
        this.map = new Object2DoubleOpenCustomHashMap<>(expected, f, STRATEGY);
    }

    private void expungeStaleEntries() {
        Object ref;
        while ((ref = queue.poll()) != null) {
            map.removeDouble(ref);
        }
    }

    public int size() {
        if (map.isEmpty()) {
            return 0;
        }
        expungeStaleEntries();
        return map.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public double getDouble(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        expungeStaleEntries();
        return map.getDouble(key);
    }

    public double getOrDefault(K key, double defaultValue) {
        Objects.requireNonNull(key, "Key cannot be null");
        expungeStaleEntries();
        return map.getOrDefault(key, defaultValue);
    }

    public double put(K key, double value) {
        Objects.requireNonNull(key, "Key cannot be null");
        expungeStaleEntries();
        WeakKey<K> weakKey = new WeakKey<>(key, queue);
        return map.put(weakKey, value);
    }

    public double remove(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        expungeStaleEntries();
        return map.removeDouble(key);
    }

    public boolean containsKey(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        expungeStaleEntries();
        return map.containsKey(key);
    }

    public void clear() {
        map.clear();
        while (queue.poll() != null) {
            // Discard
        }
    }

    public void defaultReturnValue(double rv) {
        map.defaultReturnValue(rv);
    }

    public double defaultReturnValue() {
        return map.defaultReturnValue();
    }

    private static class WeakKey<K> extends WeakReference<K> {
        private final int hash;

        public WeakKey(K key, ReferenceQueue<K> queue) {
            super(key, queue);
            this.hash = key.hashCode();
        }
    }
}
