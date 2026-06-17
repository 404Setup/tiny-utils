package one.pkg.tinyutils.map;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("all")
public class WeakConcurrentHashMap<K, V> implements ConcurrentMap<K, V> {

    private final ConcurrentMap<WeakKey<K>, V> target = new ConcurrentHashMap<>();
    private final ReferenceQueue<K> queue = new ReferenceQueue<>();

    // Bolt: Optimization - Restrict cleanup to write operations to prevent queue.poll() contention on reads
    @SuppressWarnings("unchecked")
    private void cleanup() {
        WeakKey<K> ref;
        while ((ref = (WeakKey<K>) queue.poll()) != null) {
            target.remove(ref);
        }
    }

    @Override
    public int size() {
        return target.size();
    }

    @Override
    public boolean isEmpty() {
        return target.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) return false;
        return target.containsKey(new WeakKey<>(key, null));
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) return false;
        return target.containsValue(value);
    }

    @Override
    public V get(Object key) {
        if (key == null) return null;
        return target.get(new WeakKey<Object>(key, null));
    }

    @Override
    public V put(@NotNull K key, @NotNull V value) {
        cleanup();
        if (key == null || value == null) throw new NullPointerException();
        return target.put(new WeakKey<>(key, queue), value);
    }

    @Override
    public V remove(Object key) {
        cleanup();
        if (key == null) return null;
        return target.remove(new WeakKey<>(key, null));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        cleanup();
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        target.clear();
        while (queue.poll() != null) {
            // clear the reference queue
        }
    }

    @Override
    public @NotNull Set<K> keySet() {
        Set<K> keys = new HashSet<>();
        for (WeakKey<K> weakKey : target.keySet()) {
            K key = weakKey.get();
            if (key != null) {
                keys.add(key);
            }
        }
        return keys;
    }

    @Override
    public @NotNull Collection<V> values() {
        Collection<V> vals = new ArrayList<>();
        for (Entry<WeakKey<K>, V> entry : target.entrySet()) {
            if (entry.getKey().get() != null) {
                vals.add(entry.getValue());
            }
        }
        return vals;
    }

    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> entries = new HashSet<>();
        for (Entry<WeakKey<K>, V> entry : target.entrySet()) {
            K key = entry.getKey().get();
            if (key != null) {
                entries.add(new AbstractMap.SimpleEntry<>(key, entry.getValue()));
            }
        }
        return entries;
    }

    @Override
    public V putIfAbsent(@NotNull K key, @NotNull V value) {
        cleanup();
        if (key == null || value == null) throw new NullPointerException();
        return target.putIfAbsent(new WeakKey<>(key, queue), value);
    }

    @Override
    public boolean remove(@NotNull Object key, @NotNull Object value) {
        cleanup();
        if (key == null || value == null) return false;
        return target.remove(new WeakKey<Object>(key, null), value);
    }

    @Override
    public boolean replace(@NotNull K key, @NotNull V oldValue, @NotNull V newValue) {
        cleanup();
        if (key == null || oldValue == null || newValue == null) return false;
        return target.replace(new WeakKey<>(key, null), oldValue, newValue);
    }

    @Override
    public V replace(@NotNull K key, @NotNull V value) {
        cleanup();
        if (key == null || value == null) throw new NullPointerException();
        return target.replace(new WeakKey<>(key, null), value);
    }

    private static class WeakKey<K> extends WeakReference<K> {
        private final int hash;

        public WeakKey(K referent, ReferenceQueue<? super K> q) {
            super(referent, q);
            this.hash = referent != null ? referent.hashCode() : 0;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj instanceof WeakKey<?>) {
                Object myObj = this.get();
                Object theirObj = ((WeakKey<?>) obj).get();
                if (myObj == null || theirObj == null) return false;
                return myObj.equals(theirObj);
            }
            return false;
        }
    }
}
