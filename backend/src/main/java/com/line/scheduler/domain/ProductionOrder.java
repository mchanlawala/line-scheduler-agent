package com.line.scheduler.domain;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductionOrder {
    private String productionOrderId;
    private String salesOrderId;
    private String material;
    private Integer plannedQuantity;
    private LocalDateTime basicStartDate;
    private LocalDateTime basicFinishDate;
    private LocalDateTime actualStartDate;
    private LocalDateTime actualFinishDate;
    private String status;
    private List<Operation> operations;
    private List<ProductionOrderComponent> components;
    private Long totalLeadTimeMinutes;
    private String delayReason;
}