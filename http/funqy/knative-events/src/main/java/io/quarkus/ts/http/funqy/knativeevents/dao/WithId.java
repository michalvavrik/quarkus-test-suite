package io.quarkus.ts.http.funqy.knativeevents.dao;

abstract class WithId {

    private final Integer id;

    protected WithId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}
