package com.line.scheduler.domain;

import lombok.Data;

@Data
public class ScheduleSummary {
    private int totalOrders;
    private int totalPanels;
    private double avgLeadTimeHours;
    private String allCompleteBy;
    private int linesUtilized;
}