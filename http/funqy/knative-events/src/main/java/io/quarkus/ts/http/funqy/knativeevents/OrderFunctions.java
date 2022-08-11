package io.quarkus.ts.http.funqy.knativeevents;

import io.quarkus.funqy.Funq;
import io.quarkus.ts.http.funqy.knativeevents.dao.Order;
import io.quarkus.ts.http.funqy.knativeevents.dao.OrderRepository;

public class OrderFunctions {

    private final OrderRepository orderRepository = new OrderRepository();

    @Funq
    public void reserve(Order order) {
        // FIXME: do something when order is already there, is there a response status? or trigger other event?
        // FIXME: it would be for the best if the fun accepted order + customer + stock item; is it possible?
        // FIXME: base on that, make it either confirmed or not (that must not come from a customer)
        orderRepository.add(order);
    }

}
