package com.line.scheduler.domain;

import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Operation {
    private Integer operationId;
    private Double totalHours;
    private String workCenter;
    private String description;
    private Double setupHours;
    private Double processingHours;
    private LocalDateTime plannedStart;
    private LocalDateTime plannedFinish;
    private Boolean isDelayed;
    private String delayReason;
}