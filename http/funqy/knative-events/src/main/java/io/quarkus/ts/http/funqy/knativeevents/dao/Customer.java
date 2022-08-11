package io.quarkus.ts.http.funqy.knativeevents.dao;

public class Customer extends WithId {

    public Customer(Integer id) {
        super(id);
    }

    public boolean isActive() {
        return true;
    }
}
