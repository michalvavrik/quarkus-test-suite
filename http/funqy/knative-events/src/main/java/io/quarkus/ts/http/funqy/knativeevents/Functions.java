package io.quarkus.ts.http.funqy.knativeevents;


import io.quarkus.funqy.Funq;
import io.quarkus.ts.http.funqy.knativeevents.dao.Customer;
import io.quarkus.ts.http.funqy.knativeevents.dao.CustomerRepository;
import io.quarkus.ts.http.funqy.knativeevents.dao.Order;
import io.quarkus.ts.http.funqy.knativeevents.dao.OrderRepository;
import io.quarkus.ts.http.funqy.knativeevents.dao.StockRepository;

public class Functions {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;

    public Functions(CustomerRepository customerRepository, OrderRepository orderRepository, StockRepository stockRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.stockRepository = stockRepository;
    }

    @Funq
    public Order newOrder() {
        return new Order(123456789, 1, 987654321, false);
    }

    @Funq
    public void reserve(Order order) {
        // TODO
    }

    public Customer customerService(Order order) {
        return "customerService";
    }

    public String productService() {
        return "productService";
    }

    public String orderService() {
        return "orderService";
    }

}
