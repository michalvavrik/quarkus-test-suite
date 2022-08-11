package io.quarkus.ts.http.funqy.knativeevents;

import io.quarkus.funqy.Funq;
import io.quarkus.ts.http.funqy.knativeevents.dao.Order;
import io.quarkus.ts.http.funqy.knativeevents.dao.StockRepository;

public class StockFunctions {

    private final StockRepository stockRepository;

    public StockFunctions(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Funq
    Integer getStockItems(Order order) {
        // FIXME: do this reactively!
        return stockRepository.getById(order.getProductId()).getNumOfItems();
    }

}
