package one.pkg.tinyutils.map;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;

@SuppressWarnings("all")
public class WeakLinkedHashMap<K, V> implements Map<K, V> {

    private final Map<WeakKey<K>, V> target;
    private final ReferenceQueue<K> queue = new ReferenceQueue<>();

    public WeakLinkedHashMap() {
        this.target = new LinkedHashMap<>();
    }

    public WeakLinkedHashMap(int initialCapacity) {
        this.target = new LinkedHashMap<>(initialCapacity);
    }

    public WeakLinkedHashMap(int initialCapacity, float loadFactor) {
        this.target = new LinkedHashMap<>(initialCapacity, loadFactor);
    }

    public WeakLinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
        this.target = new LinkedHashMap<>(initialCapacity, loadFactor, accessOrder);
    }

    public WeakLinkedHashMap(Map<? extends K, ? extends V> m) {
        this.target = new LinkedHashMap<>(Math.max((int) (m.size() / 0.75f) + 1, 16));
        putAll(m);
    }

    @SuppressWarnings("unchecked")
    private void cleanup() {
        WeakKey<K> ref;
        while ((ref = (WeakKey<K>) queue.poll()) != null) {
            target.remove(ref);
        }
    }

    @Override
    public int size() {
        cleanup();
        return target.size();
    }

    @Override
    public boolean isEmpty() {
        cleanup();
        return target.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        cleanup();
        if (key == null) return false;
        return target.containsKey(new WeakKey<>(key, null));
    }

    @Override
    public boolean containsValue(Object value) {
        cleanup();
        if (value == null) return false;
        return target.containsValue(value);
    }

    @Override
    public V get(Object key) {
        cleanup();
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
        cleanup();
        Set<K> keys = new LinkedHashSet<>();
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
        cleanup();
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
        cleanup();
        Set<Entry<K, V>> entries = new LinkedHashSet<>();
        for (Entry<WeakKey<K>, V> entry : target.entrySet()) {
            K key = entry.getKey().get();
            if (key != null) {
                entries.add(new AbstractMap.SimpleEntry<>(key, entry.getValue()));
            }
        }
        return entries;
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
