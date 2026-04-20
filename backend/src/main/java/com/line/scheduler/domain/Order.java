package com.line.scheduler.domain;

import lombok.Data;
import java.util.List;

@Data
public class Order {
    private String orderId;
    private String panelType;
    private int wattage;
    private int quantity;
    private String orderReceived;
    private String dueDate;
    private int priority;
    private boolean customSetup;
    private String estimatedCompletion;
    private String status;           // SCHEDULED, DELAYED, COMPLETED, PENDING
    private int totalLeadTimeMinutes;
    private List<StepExecution> steps;
}