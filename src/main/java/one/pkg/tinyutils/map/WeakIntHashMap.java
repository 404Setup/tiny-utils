package one.pkg.tinyutils.map;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * A Map-like data structure that uses primitive int keys and weakly references its values.
 * Similar to {@link java.util.WeakHashMap}, an entry is automatically removed when its value
 * is no longer ordinarily reachable.
 *
 * @param <V> the type of mapped values
 */
public class WeakIntHashMap<V> {

    private final Int2ObjectOpenHashMap<WeakValue<V>> map;
    private final ReferenceQueue<V> queue = new ReferenceQueue<>();

    public WeakIntHashMap() {
        this.map = new Int2ObjectOpenHashMap<>();
    }

    public WeakIntHashMap(int expected) {
        this.map = new Int2ObjectOpenHashMap<>(expected);
    }

    public WeakIntHashMap(int expected, float f) {
        this.map = new Int2ObjectOpenHashMap<>(expected, f);
    }

    /**
     * Expunges stale entries from the map.
     */
    private void expungeStaleEntries() {
        WeakValue<V> ref;
        while ((ref = (WeakValue<V>) queue.poll()) != null) {
            WeakValue<V> current = map.get(ref.getKey());
            if (current == ref) {
                map.remove(ref.getKey());
            }
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
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     */
    @Nullable
    public V get(int key) {
        expungeStaleEntries();
        WeakValue<V> ref = map.get(key);
        if (ref == null) {
            return null;
        }
        V value = ref.get();
        if (value == null) {
            map.remove(key);
            return null;
        }
        return value;
    }

    /**
     * Associates the specified value with the specified key in this map.
     */
    public V put(int key, V value) {
        Objects.requireNonNull(value, "Value cannot be null");
        expungeStaleEntries();
        WeakValue<V> ref = new WeakValue<>(key, value, queue);
        WeakValue<V> oldRef = map.put(key, ref);
        if (oldRef != null) {
            V oldValue = oldRef.get();
            oldRef.clear();
            return oldValue;
        }
        return null;
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     */
    public V remove(int key) {
        expungeStaleEntries();
        WeakValue<V> oldRef = map.remove(key);
        if (oldRef != null) {
            V oldValue = oldRef.get();
            oldRef.clear();
            return oldValue;
        }
        return null;
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     */
    public boolean containsKey(int key) {
        expungeStaleEntries();
        WeakValue<V> ref = map.get(key);
        if (ref != null) {
            if (ref.get() != null) {
                return true;
            } else {
                map.remove(key);
            }
        }
        return false;
    }

    /**
     * Removes all of the mappings from this map.
     */
    public void clear() {
        for (WeakValue<V> ref : map.values()) {
            if (ref != null) {
                ref.clear();
            }
        }
        map.clear();
        while (queue.poll() != null) {
            // Discard
        }
    }

    private static class WeakValue<V> extends WeakReference<V> {
        private final int key;

        public WeakValue(int key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            this.key = key;
        }

        public int getKey() {
            return key;
        }
    }
}
