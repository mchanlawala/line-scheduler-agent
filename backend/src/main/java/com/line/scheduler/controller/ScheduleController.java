package com.line.scheduler.controller;

import com.line.scheduler.domain.*;
import com.line.scheduler.service.ProductionOrderReaderService;
import com.line.scheduler.agent.InsightsAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ProductionOrderReaderService readerService;
    private final InsightsAgent insightsAgent;

    @GetMapping("/result")
    public ResponseEntity<ScheduleResult> getResult() {
        ScheduleResult result = readerService.getScheduleResult();
        String insights = insightsAgent.analyseSchedule(result);
        result.setAiInsights(insights);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(readerService.getScheduleResult().getOrders());
    }

    @GetMapping("/gantt/{orderId}")
    public ResponseEntity<List<StepExecution>> getGanttData(@PathVariable String orderId) {
        return ResponseEntity.ok(readerService.getStepsForOrder(orderId));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardData> getDashboard() {
        return ResponseEntity.ok(readerService.buildDashboard());
    }

    @GetMapping("/insights")
    public ResponseEntity<Map<String, String>> getInsights() {
        String insights = insightsAgent.analyseSchedule(readerService.getScheduleResult());
        return ResponseEntity.ok(Map.of("insights", insights));
    }

    @GetMapping("/line/{lineId}")
    public ResponseEntity<List<StepExecution>> getLineSchedule(@PathVariable String lineId) {
        return ResponseEntity.ok(readerService.getStepsForLine(lineId));
    }

    @GetMapping("/lines/summary")
    public ResponseEntity<Map<String, Object>> getLinesSummary() {
        return ResponseEntity.ok(readerService.getLinesSummary());
    }

    @GetMapping("/line/{lineId}/batches")
    public ResponseEntity<List<BatchExecution>> getLineBatches(@PathVariable String lineId) {
        return ResponseEntity.ok(readerService.getBatchesForLine(lineId));
    }

    @GetMapping("/lines/batches-summary")
    public ResponseEntity<Map<String, Object>> getLinesBatchSummary() {
        return ResponseEntity.ok(readerService.getLinesBatchSummary());
    }
}