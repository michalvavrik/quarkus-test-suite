package io.quarkus.ts.http.funqy.knativeevents;

import io.quarkus.funqy.Funq;
import io.quarkus.ts.http.funqy.knativeevents.dao.Customer;
import io.quarkus.ts.http.funqy.knativeevents.dao.CustomerRepository;
import io.quarkus.ts.http.funqy.knativeevents.dao.Order;

import javax.inject.Inject;

public class CustomerFunctions {

    @Inject
    CustomerRepository customerRepository;

    @Funq
    public Customer getCustomer(Order order) {
        return customerRepository.getById(order.getCustomerId());
    }

}
