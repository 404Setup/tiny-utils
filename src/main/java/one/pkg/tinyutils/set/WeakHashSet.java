package one.pkg.tinyutils.set;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.WeakHashMap;

public class WeakHashSet<E> extends AbstractSet<E> {
    private final transient WeakHashMap<E, Object> map;
    private static final Object PRESENT = new Object();

    public WeakHashSet() {
        map = new WeakHashMap<>();
    }

    public WeakHashSet(Collection<? extends E> c) {
        map = new WeakHashMap<>(Math.max((int) (c.size() / .75f) + 1, 16));
        addAll(c);
    }

    public WeakHashSet(int initialCapacity, float loadFactor) {
        map = new WeakHashMap<>(initialCapacity, loadFactor);
    }

    public WeakHashSet(int initialCapacity) {
        map = new WeakHashMap<>(initialCapacity);
    }

    @Override
    public @NotNull Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }

    @Override
    public void clear() {
        map.clear();
    }
}
