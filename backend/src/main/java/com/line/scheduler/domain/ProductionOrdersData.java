package com.line.scheduler.domain;

import lombok.Data;
import java.util.List;

@Data
public class ProductionOrdersData {
    private List<ProductionOrder> productionOrders;
    private Summary summary;
    private String aiInsights;

    @Data
    public static class Summary {
        private int totalProductionOrders;
        private int delayedOrders;
        private int totalPanels;
    }
}