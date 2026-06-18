/*
 * Copyright (C) 2026  404Setup.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package one.pkg.tinyutils.map;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * A Map-like data structure that uses primitive long keys and weakly references its values.
 * Similar to {@link java.util.WeakHashMap}, an entry is automatically removed when its value
 * is no longer ordinarily reachable.
 *
 * @param <V> the type of mapped values
 */
public class WeakLongHashMap<V> {

    private final Long2ObjectOpenHashMap<WeakValue<V>> map;
    private final ReferenceQueue<V> queue = new ReferenceQueue<>();

    public WeakLongHashMap() {
        this.map = new Long2ObjectOpenHashMap<>();
    }

    public WeakLongHashMap(int expected) {
        this.map = new Long2ObjectOpenHashMap<>(expected);
    }

    public WeakLongHashMap(int expected, float f) {
        this.map = new Long2ObjectOpenHashMap<>(expected, f);
    }

    /**
     * Expunges stale entries from the map.
     */
    // Bolt: Optimization - Restrict cleanup to write operations to prevent queue.poll() contention on reads
    private void expungeStaleEntries() {
        WeakValue<V> ref;
        while ((ref = (WeakValue<V>) queue.poll()) != null) {
            // Only remove if it's the exact same reference that was polled.
            // If the key was overwritten with a new value, the new reference would be different.
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
    public V get(long key) {
        WeakValue<V> ref = map.get(key);
        if (ref == null) {
            return null;
        }
        V value = ref.get();
        if (value == null) {
            // Value was GC'd, wait for expunge or proactively remove it here
            map.remove(key);
            return null;
        }
        return value;
    }

    /**
     * Associates the specified value with the specified key in this map.
     */
    public V put(long key, V value) {
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
    public V remove(long key) {
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
    public boolean containsKey(long key) {
        WeakValue<V> ref = map.get(key);
        if (ref != null) {
            if (ref.get() != null) {
                return true;
            } else {
                map.remove(key); // Proactively clean up
            }
        }
        return false;
    }

    /**
     * Removes all of the mappings from this map.
     */
    public void clear() {
        // Enqueue remaining references just in case, though they'll be garbage collected anyway.
        for (WeakValue<V> ref : map.values()) {
            if (ref != null) {
                ref.clear();
            }
        }
        map.clear();
        // Clear the queue
        while (queue.poll() != null) {
            // Discard
        }
    }

    private static class WeakValue<V> extends WeakReference<V> {
        private final long key;

        public WeakValue(long key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            this.key = key;
        }

        public long getKey() {
            return key;
        }
    }
}
