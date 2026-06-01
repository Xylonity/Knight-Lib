package dev.xylonity.knightlib.api.registrar;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class ResourceEntries<T> {

    private final List<ResourceEntry<T>> entries = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public <I extends T> ResourceEntry<I> add(ResourceEntry<I> entry) {
        entries.add((ResourceEntry<T>) entry);
        return entry;
    }

    public List<ResourceEntry<T>> getAllEntries() {
        return ImmutableList.copyOf(entries);
    }

}