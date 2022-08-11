package io.quarkus.ts.http.funqy.knativeevents.dao;

public class StockItem extends WithId {

    private final Integer numOfItems;

    public StockItem(Integer id, Integer numOfItems) {
        super(id);
        this.numOfItems = numOfItems;
    }

    public Integer getNumOfItems() {
        return numOfItems;
    }
}
