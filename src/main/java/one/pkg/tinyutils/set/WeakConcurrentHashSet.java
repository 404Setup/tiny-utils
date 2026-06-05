package one.pkg.tinyutils.set;

import one.pkg.tinyutils.map.WeakConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

public class WeakConcurrentHashSet<E> extends AbstractSet<E> {
    private final transient WeakConcurrentHashMap<E, Object> map;
    private static final Object PRESENT = new Object();

    public WeakConcurrentHashSet() {
        map = new WeakConcurrentHashMap<>();
    }

    public WeakConcurrentHashSet(Collection<? extends E> c) {
        map = new WeakConcurrentHashMap<>();
        addAll(c);
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
