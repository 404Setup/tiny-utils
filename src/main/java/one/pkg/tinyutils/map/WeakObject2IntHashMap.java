package one.pkg.tinyutils.map;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * A Map-like data structure that uses weakly referenced Object keys and primitive int values.
 * Similar to {@link java.util.WeakHashMap}, an entry is automatically removed when its key
 * is no longer ordinarily reachable.
 *
 * @param <K> the type of keys maintained by this map
 */
public class WeakObject2IntHashMap<K> {
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

    private final Object2IntOpenCustomHashMap<Object> map;
    private final ReferenceQueue<K> queue = new ReferenceQueue<>();

    public WeakObject2IntHashMap() {
        this.map = new Object2IntOpenCustomHashMap<>(STRATEGY);
    }

    public WeakObject2IntHashMap(int expected) {
        this.map = new Object2IntOpenCustomHashMap<>(expected, STRATEGY);
    }

    public WeakObject2IntHashMap(int expected, float f) {
        this.map = new Object2IntOpenCustomHashMap<>(expected, f, STRATEGY);
    }

    /**
     * Expunges stale entries from the map.
     */
    private void expungeStaleEntries() {
        Object ref;
        while ((ref = queue.poll()) != null) {
            map.removeInt(ref);
        }
    }

    /**
     * Returns the number of key-value mappings in this map.
     */
    public int size() {
        if (map.isEmpty()) {
            return 0;
        }
        expungeStaleEntries();
        return map.size();
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns the value to which the specified key is mapped.
     * Returns the map's default return value (usually 0) if this map contains no mapping for the key.
     */
    public int getInt(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        expungeStaleEntries();
        return map.getInt(key);
    }

    /**
     * Returns the value to which the specified key is mapped, or the given default value
     * if this map contains no mapping for the key.
     */
    public int getOrDefault(K key, int defaultValue) {
        Objects.requireNonNull(key, "Key cannot be null");
        expungeStaleEntries();
        return map.getOrDefault(key, defaultValue);
    }

    /**
     * Associates the specified value with the specified key in this map.
     *
     * @return the previous value associated with {@code key}, or the default value (usually 0) if there was no mapping for {@code key}.
     */
    public int put(K key, int value) {
        Objects.requireNonNull(key, "Key cannot be null");
        expungeStaleEntries();
        WeakKey<K> weakKey = new WeakKey<>(key, queue);
        return map.put(weakKey, value);
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @return the previous value associated with {@code key}, or the default value (usually 0) if there was no mapping for {@code key}.
     */
    public int remove(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        expungeStaleEntries();
        return map.removeInt(key);
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     */
    public boolean containsKey(K key) {
        Objects.requireNonNull(key, "Key cannot be null");
        expungeStaleEntries();
        return map.containsKey(key);
    }

    /**
     * Removes all of the mappings from this map.
     */
    public void clear() {
        map.clear();
        while (queue.poll() != null) {
            // Discard
        }
    }

    /**
     * Sets the default return value for this map.
     */
    public void defaultReturnValue(int rv) {
        map.defaultReturnValue(rv);
    }

    /**
     * Gets the default return value for this map.
     */
    public int defaultReturnValue() {
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
