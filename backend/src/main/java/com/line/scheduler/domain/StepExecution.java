package com.line.scheduler.domain;

import lombok.Data;
import java.util.List;

@Data
public class StepExecution {
    private int stepSeq;
    private String processName;
    private String lineId;
    private String plannedStart;
    private String plannedEnd;
    private int durationMinutes;
    private boolean isDelayed;
    private String delayReason;
    private String color;
    private String orderId;
    private List<BatchExecution> batches;
}