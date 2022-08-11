package io.quarkus.ts.http.funqy.knativeevents.dao;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AbstractRepository<T extends WithId> {

    private final Map<Integer, T> repository = new ConcurrentHashMap<>();

    public void add(T item) {
        repository.put(item.getId(), item);
    }

    public void remove(T item) {
        repository.remove(item.getId());
    }

    public T getById(Integer id) {
        return repository.get(id);
    }

}
