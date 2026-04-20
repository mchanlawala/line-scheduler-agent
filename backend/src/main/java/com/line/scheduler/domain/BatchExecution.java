package com.line.scheduler.domain;

import lombok.Data;

@Data
public class BatchExecution {
    private String orderId;
    private int stepSeq;
    private String processName;
    private String lineId;
    private int batchNumber;
    private int batchQuantity;
    private String plannedStart;
    private String plannedEnd;
    private int durationMinutes;
    private boolean isDelayed;
    private String delayReason;
    private String color;
}