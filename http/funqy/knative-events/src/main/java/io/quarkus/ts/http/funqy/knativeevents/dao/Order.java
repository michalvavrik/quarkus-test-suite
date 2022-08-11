package io.quarkus.ts.http.funqy.knativeevents.dao;


public class Order extends WithId {

    private final Integer productId;

    private final Integer numOfItems;

    private final Integer customerId;

    private final boolean confirmed;

    public Order(Integer productId, Integer numOfItems, Integer customerId, boolean confirmed) {
        super((int) (Math.random() * 1000));
        this.productId = productId;
        this.numOfItems = numOfItems;
        this.customerId = customerId;
        this.confirmed = confirmed;
    }

    public Integer getProductId() {
        return productId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public Integer getNumOfItems() {
        return numOfItems;
    }
}
