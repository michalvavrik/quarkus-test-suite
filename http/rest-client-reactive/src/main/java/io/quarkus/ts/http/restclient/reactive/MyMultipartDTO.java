package io.quarkus.ts.http.restclient.reactive;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class MyMultipartDTO {
    private List<Item> items;

    public MyMultipartDTO() {
    }

    public MyMultipartDTO(List<Item> items) {
        this.items = items;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
