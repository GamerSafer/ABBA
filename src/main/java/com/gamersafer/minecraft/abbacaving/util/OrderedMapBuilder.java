package com.gamersafer.minecraft.abbacaving.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OrderedMapBuilder<K, V> {

    private final Map<K, V> map = new LinkedHashMap<>();

    public OrderedMapBuilder() {

    }

    public OrderedMapBuilder<K, V> add(final K key, final V value) {
        this.map.put(key, value);

        return this;
    }

    public Map<K, V> build() {
        return this.map;
    }

}
