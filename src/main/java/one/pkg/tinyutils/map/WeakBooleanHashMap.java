package one.pkg.tinyutils.map;

import org.jetbrains.annotations.Nullable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * A Map-like data structure that uses primitive boolean keys and weakly references its values.
 * Since there are only two possible boolean values, this implementation simply uses two references
 * internally and is highly optimized.
 *
 * @param <V> the type of mapped values
 */
public class WeakBooleanHashMap<V> {

    private WeakValue<V> trueRef;
    private WeakValue<V> falseRef;
    private final ReferenceQueue<V> queue = new ReferenceQueue<>();

    public WeakBooleanHashMap() {
    }

    private void expungeStaleEntries() {
        WeakValue<V> ref;
        while ((ref = (WeakValue<V>) queue.poll()) != null) {
            if (ref.key) {
                if (trueRef == ref) trueRef = null;
            } else {
                if (falseRef == ref) falseRef = null;
            }
        }
    }

    public int size() {
        expungeStaleEntries();
        int count = 0;
        if (trueRef != null && trueRef.get() != null) count++;
        else trueRef = null;
        if (falseRef != null && falseRef.get() != null) count++;
        else falseRef = null;
        return count;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    @Nullable
    public V get(boolean key) {
        expungeStaleEntries();
        WeakValue<V> ref = key ? trueRef : falseRef;
        if (ref == null) {
            return null;
        }
        V value = ref.get();
        if (value == null) {
            if (key) trueRef = null;
            else falseRef = null;
            return null;
        }
        return value;
    }

    public V put(boolean key, V value) {
        Objects.requireNonNull(value, "Value cannot be null");
        expungeStaleEntries();
        WeakValue<V> newRef = new WeakValue<>(key, value, queue);
        WeakValue<V> oldRef = key ? trueRef : falseRef;
        
        if (key) {
            trueRef = newRef;
        } else {
            falseRef = newRef;
        }
        
        if (oldRef != null) {
            V oldValue = oldRef.get();
            oldRef.clear();
            return oldValue;
        }
        return null;
    }

    public V remove(boolean key) {
        expungeStaleEntries();
        WeakValue<V> oldRef = key ? trueRef : falseRef;
        if (oldRef != null) {
            V oldValue = oldRef.get();
            oldRef.clear();
            if (key) trueRef = null;
            else falseRef = null;
            return oldValue;
        }
        return null;
    }

    public boolean containsKey(boolean key) {
        expungeStaleEntries();
        WeakValue<V> ref = key ? trueRef : falseRef;
        if (ref != null) {
            if (ref.get() != null) {
                return true;
            } else {
                if (key) trueRef = null;
                else falseRef = null;
            }
        }
        return false;
    }

    public void clear() {
        if (trueRef != null) trueRef.clear();
        if (falseRef != null) falseRef.clear();
        trueRef = null;
        falseRef = null;
        while (queue.poll() != null) {
            // Discard
        }
    }

    private static class WeakValue<V> extends WeakReference<V> {
        private final boolean key;

        public WeakValue(boolean key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            this.key = key;
        }
    }
}
