package one.pkg.tinyutils.map;

import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * A Map-like data structure that uses primitive float keys and weakly references its values.
 * Similar to {@link java.util.WeakHashMap}, an entry is automatically removed when its value
 * is no longer ordinarily reachable.
 *
 * @param <V> the type of mapped values
 */
public class WeakFloatHashMap<V> {

    private final Float2ObjectOpenHashMap<WeakValue<V>> map;
    private final ReferenceQueue<V> queue = new ReferenceQueue<>();

    public WeakFloatHashMap() {
        this.map = new Float2ObjectOpenHashMap<>();
    }

    public WeakFloatHashMap(int expected) {
        this.map = new Float2ObjectOpenHashMap<>(expected);
    }

    public WeakFloatHashMap(int expected, float f) {
        this.map = new Float2ObjectOpenHashMap<>(expected, f);
    }

    // Bolt: Optimization - Restrict cleanup to write operations to prevent queue.poll() contention on reads
    private void expungeStaleEntries() {
        WeakValue<V> ref;
        while ((ref = (WeakValue<V>) queue.poll()) != null) {
            WeakValue<V> current = map.get(ref.getKey());
            if (current == ref) {
                map.remove(ref.getKey());
            }
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

    @Nullable
    public V get(float key) {
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

    public V put(float key, V value) {
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

    public V remove(float key) {
        expungeStaleEntries();
        WeakValue<V> oldRef = map.remove(key);
        if (oldRef != null) {
            V oldValue = oldRef.get();
            oldRef.clear();
            return oldValue;
        }
        return null;
    }

    public boolean containsKey(float key) {
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
        private final float key;

        public WeakValue(float key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            this.key = key;
        }

        public float getKey() {
            return key;
        }
    }
}
