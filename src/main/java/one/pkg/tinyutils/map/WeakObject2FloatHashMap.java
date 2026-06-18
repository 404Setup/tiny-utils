package one.pkg.tinyutils.map;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenCustomHashMap;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * A Map-like data structure that uses weakly referenced Object keys and primitive float values.
 * Similar to {@link java.util.WeakHashMap}, an entry is automatically removed when its key
 * is no longer ordinarily reachable.
 *
 * @param <K> the type of keys maintained by this map
 */
public class WeakObject2FloatHashMap<K> {
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

    private final Object2FloatOpenCustomHashMap<Object> map;
    private final ReferenceQueue<K> queue = new ReferenceQueue<>();

    public WeakObject2FloatHashMap() {
        this.map = new Object2FloatOpenCustomHashMap<>(STRATEGY);
    }

    public WeakObject2FloatHashMap(int expected) {
        this.map = new Object2FloatOpenCustomHashMap<>(expected, STRATEGY);
    }

    public WeakObject2FloatHashMap(int expected, float f) {
        this.map = new Object2FloatOpenCustomHashMap<>(expected, f, STRATEGY);
    }

    // Bolt: Optimization - Restrict cleanup to write operations to prevent queue.poll() contention on reads
    private void expungeStaleEntries() {
        Object ref;
        while ((ref = queue.poll()) != null) {
            map.removeFloat(ref);
        }
    }

    public int size() {
        if (map.isEmpty()) {
            return 0;
        }
        return map.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public float getFloat(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return map.getFloat(key);
    }

    public float getOrDefault(K key, float defaultValue) {
        Objects.requireNonNull(key, "Key cannot be null");
        return map.getOrDefault(key, defaultValue);
    }

    public float put(K key, float value) {
        Objects.requireNonNull(key, "Key cannot be null");
        expungeStaleEntries();
        WeakKey<K> weakKey = new WeakKey<>(key, queue);
        return map.put(weakKey, value);
    }

    public float remove(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        expungeStaleEntries();
        return map.removeFloat(key);
    }

    public boolean containsKey(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return map.containsKey(key);
    }

    public void clear() {
        map.clear();
        while (queue.poll() != null) {
            // Discard
        }
    }

    public void defaultReturnValue(float rv) {
        map.defaultReturnValue(rv);
    }

    public float defaultReturnValue() {
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
