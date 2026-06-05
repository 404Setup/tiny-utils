package one.pkg.tinyutils.set;

import one.pkg.tinyutils.map.WeakLinkedHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

public class WeakLinkedHashSet<E> extends AbstractSet<E> {
    private final transient WeakLinkedHashMap<E, Object> map;
    private static final Object PRESENT = new Object();

    public WeakLinkedHashSet() {
        map = new WeakLinkedHashMap<>();
    }

    public WeakLinkedHashSet(Collection<? extends E> c) {
        map = new WeakLinkedHashMap<>(Math.max((int) (c.size() / .75f) + 1, 16));
        addAll(c);
    }

    public WeakLinkedHashSet(int initialCapacity, float loadFactor) {
        map = new WeakLinkedHashMap<>(initialCapacity, loadFactor);
    }

    public WeakLinkedHashSet(int initialCapacity) {
        map = new WeakLinkedHashMap<>(initialCapacity);
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
