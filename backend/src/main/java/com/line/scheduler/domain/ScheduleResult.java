package com.line.scheduler.domain;

import lombok.Data;
import java.util.List;

@Data
public class ScheduleResult {
    private List<Order> orders;
    private String generatedAt;
    private String aiInsights;
    private ScheduleSummary summary;
}