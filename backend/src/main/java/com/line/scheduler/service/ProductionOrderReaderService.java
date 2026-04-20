package com.line.scheduler.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.line.scheduler.domain.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductionOrderReaderService {

    private static final String DATA_DIR = "./data/";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private List<ProductionOrder> productionOrders;
    private ScheduleResult cachedResult;

    // Max batch sizes per work centre
    private static final Map<String, Integer> WORK_CENTER_MAX_BATCH = Map.of(
        "CELL-TEST-01", Integer.MAX_VALUE,
        "LASER-01", Integer.MAX_VALUE,
        "STRINGER-02", Integer.MAX_VALUE,
        "EL-TEST-01", 1000,
        "LAMINATOR-03", 1000,
        "FRAMING-01", Integer.MAX_VALUE,
        "FLASH-TEST-01", 500,
        "EL-TEST-02", 1000,
        "PACK-01", 30
    );

    @PostConstruct
    public void init() throws IOException {
        loadProductionOrders();
        buildScheduleResult();
    }

    private void loadProductionOrders() throws IOException {
        File file = new File(DATA_DIR + "production_orders.json");
        if (!file.exists()) {
            throw new IOException("production_orders.json not found in " + DATA_DIR);
        }
        ProductionOrdersData data = mapper.readValue(file, ProductionOrdersData.class);
        productionOrders = data.getProductionOrders();
        log.info("Loaded {} production orders", productionOrders.size());
    }

    public ScheduleResult getScheduleResult() {
        if (cachedResult == null) {
            buildScheduleResult();
        }
        return cachedResult;
    }

    private synchronized void buildScheduleResult() {
        List<Order> orders = new ArrayList<>();
        for (ProductionOrder po : productionOrders) {
            Order order = convertToOrder(po);
            orders.add(order);
        }

        ScheduleSummary summary = computeSummary(orders);
        cachedResult = new ScheduleResult();
        cachedResult.setOrders(orders);
        cachedResult.setGeneratedAt(LocalDateTime.now().toString());
        cachedResult.setSummary(summary);
        cachedResult.setAiInsights(""); // Will be set by controller
    }

    private Order convertToOrder(ProductionOrder po) {
        Order order = new Order();
        order.setOrderId(po.getProductionOrderId());
        order.setPanelType(extractPanelType(po.getMaterial()));
        order.setWattage(extractWattage(po.getMaterial()));
        order.setQuantity(po.getPlannedQuantity());
        order.setOrderReceived(formatDateTime(po.getBasicStartDate()));
        order.setDueDate(formatDateTime(po.getBasicFinishDate()));
        order.setPriority(mapPriority(po.getSalesOrderId()));
        order.setCustomSetup(false);
        order.setEstimatedCompletion(formatDateTime(po.getActualFinishDate()));
        order.setStatus(mapStatus(po.getStatus()));
        order.setTotalLeadTimeMinutes(po.getTotalLeadTimeMinutes() != null ? po.getTotalLeadTimeMinutes().intValue() : 0);

        List<StepExecution> steps = new ArrayList<>();
        int stepSeq = 1;
        for (Operation op : po.getOperations()) {
            StepExecution step = new StepExecution();
            step.setStepSeq(stepSeq++);
            step.setProcessName(op.getDescription());
            step.setLineId(op.getWorkCenter());
            step.setPlannedStart(formatDateTime(op.getPlannedStart()));
            step.setPlannedEnd(formatDateTime(op.getPlannedFinish()));
            step.setDurationMinutes((int) Math.round(op.getTotalHours() * 60));
            step.setDelayed(op.getIsDelayed() != null && op.getIsDelayed());
            step.setDelayReason(op.getDelayReason());
            step.setColor(generateColor(stepSeq));
            step.setOrderId(order.getOrderId());

            // Split into batches if needed
            int maxBatch = WORK_CENTER_MAX_BATCH.getOrDefault(op.getWorkCenter(), Integer.MAX_VALUE);
            if (maxBatch < order.getQuantity()) {
                step.setBatches(splitIntoBatches(order, step, op, maxBatch));
            } else {
                step.setBatches(null);
            }

            steps.add(step);
        }
        order.setSteps(steps);
        return order;
    }

    private List<BatchExecution> splitIntoBatches(Order order, StepExecution step, Operation op, int maxBatch) {
        int totalQty = order.getQuantity();
        int numBatches = (int) Math.ceil((double) totalQty / maxBatch);
        List<BatchExecution> batches = new ArrayList<>();
        int remaining = totalQty;
        LocalDateTime start = parseDateTime(step.getPlannedStart());
        long totalDuration = step.getDurationMinutes();

        for (int i = 1; i <= numBatches; i++) {
            int batchQty = Math.min(remaining, maxBatch);
            long batchDuration = Math.round((double) batchQty / totalQty * totalDuration);
            LocalDateTime batchEnd = start.plusMinutes(batchDuration);

            BatchExecution batch = new BatchExecution();
            batch.setOrderId(order.getOrderId());
            batch.setStepSeq(step.getStepSeq());
            batch.setProcessName(step.getProcessName());
            batch.setLineId(step.getLineId());
            batch.setBatchNumber(i);
            batch.setBatchQuantity(batchQty);
            batch.setPlannedStart(formatDateTime(start));
            batch.setPlannedEnd(formatDateTime(batchEnd));
            batch.setDurationMinutes((int) batchDuration);
            batch.setDelayed(step.isDelayed());
            batch.setDelayReason(step.getDelayReason());
            batch.setColor(step.getColor());

            batches.add(batch);
            remaining -= batchQty;
            start = batchEnd;
        }
        return batches;
    }

    public DashboardData buildDashboard() {
        ScheduleResult result = getScheduleResult();
        List<Order> orders = result.getOrders();

        Map<String, Integer> ordersByStatus = orders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.summingInt(o -> 1)));

        Map<String, Double> lineUtilisation = new HashMap<>();
        for (Order order : orders) {
            for (StepExecution step : order.getSteps()) {
                lineUtilisation.merge(step.getLineId(), (double) step.getDurationMinutes(), Double::sum);
            }
        }
        double shiftMinutes = 960; // 16h shift
        lineUtilisation.replaceAll((line, mins) -> Math.min(100, (mins / shiftMinutes) * 100));

        Map<String, Integer> delaySummary = new HashMap<>();
        for (Order order : orders) {
            if ("DELAYED".equals(order.getStatus())) {
                for (StepExecution step : order.getSteps()) {
                    if (step.isDelayed() && step.getDelayReason() != null) {
                        delaySummary.merge(step.getDelayReason(), 1, Integer::sum);
                    }
                }
            }
        }

        List<Order> recentOrders = orders.stream().limit(5).collect(Collectors.toList());

        DashboardData dashboard = new DashboardData();
        dashboard.setOrdersByStatus(ordersByStatus);
        dashboard.setLineUtilisation(lineUtilisation);
        dashboard.setDelaySummary(delaySummary);
        dashboard.setRecentOrders(recentOrders);
        dashboard.setSummary(result.getSummary());
        return dashboard;
    }

    public List<StepExecution> getStepsForOrder(String orderId) {
        return getScheduleResult().getOrders().stream()
                .filter(o -> o.getOrderId().equals(orderId))
                .flatMap(o -> o.getSteps().stream())
                .collect(Collectors.toList());
    }

    public List<StepExecution> getStepsForLine(String lineId) {
        List<StepExecution> lineSteps = new ArrayList<>();
        for (Order order : getScheduleResult().getOrders()) {
            for (StepExecution step : order.getSteps()) {
                if (step.getLineId().equals(lineId)) {
                    lineSteps.add(step);
                }
            }
        }
        lineSteps.sort(Comparator.comparing(StepExecution::getPlannedStart));
        return lineSteps;
    }

    public List<BatchExecution> getBatchesForLine(String lineId) {
        List<BatchExecution> batches = new ArrayList<>();
        for (Order order : getScheduleResult().getOrders()) {
            for (StepExecution step : order.getSteps()) {
                if (step.getLineId().equals(lineId) && step.getBatches() != null) {
                    batches.addAll(step.getBatches());
                }
            }
        }
        batches.sort(Comparator.comparing(BatchExecution::getPlannedStart));
        return batches;
    }

    public Map<String, Object> getLinesSummary() {
        Map<String, Object> summary = new HashMap<>();
        Map<String, Integer> orderCountPerLine = new HashMap<>();
        Map<String, Integer> stepCountPerLine = new HashMap<>();

        for (Order order : getScheduleResult().getOrders()) {
            for (StepExecution step : order.getSteps()) {
                String line = step.getLineId();
                stepCountPerLine.merge(line, 1, Integer::sum);
                orderCountPerLine.putIfAbsent(line, orderCountPerLine.getOrDefault(line, 0) + 1);
            }
        }
        summary.put("orderCountPerLine", orderCountPerLine);
        summary.put("stepCountPerLine", stepCountPerLine);
        summary.put("totalLines", stepCountPerLine.size());
        return summary;
    }

    public Map<String, Object> getLinesBatchSummary() {
        Map<String, Object> summary = new HashMap<>();
        Map<String, Integer> batchCountPerLine = new HashMap<>();

        for (Order order : getScheduleResult().getOrders()) {
            for (StepExecution step : order.getSteps()) {
                if (step.getBatches() != null) {
                    batchCountPerLine.merge(step.getLineId(), step.getBatches().size(), Integer::sum);
                }
            }
        }
        summary.put("batchCountPerLine", batchCountPerLine);
        return summary;
    }

    private ScheduleSummary computeSummary(List<Order> orders) {
        ScheduleSummary summary = new ScheduleSummary();
        summary.setTotalOrders(orders.size());
        summary.setTotalPanels(orders.stream().mapToInt(Order::getQuantity).sum());
        double avgLead = orders.stream().mapToInt(Order::getTotalLeadTimeMinutes).average().orElse(0);
        summary.setAvgLeadTimeHours(avgLead / 60);
        String allComplete = orders.stream()
                .map(Order::getEstimatedCompletion)
                .max(String::compareTo)
                .orElse("");
        summary.setAllCompleteBy(allComplete);
        long distinctLines = orders.stream()
                .flatMap(o -> o.getSteps().stream())
                .map(StepExecution::getLineId)
                .distinct()
                .count();
        summary.setLinesUtilized((int) distinctLines);
        return summary;
    }

    // Helpers
    private String extractPanelType(String material) {
        if (material.contains("545W")) return "545W Mono";
        if (material.contains("540W")) return "540W Mono";
        if (material.contains("550W")) return "550W Mono";
        if (material.contains("530W")) return "530W Poly";
        return material;
    }

    private int extractWattage(String material) {
        if (material.contains("545W")) return 545;
        if (material.contains("540W")) return 540;
        if (material.contains("550W")) return 550;
        if (material.contains("530W")) return 530;
        return 0;
    }

    private int mapPriority(String salesOrderId) {
        // Adjust logic based on your actual priority mapping
        if (salesOrderId.equals("SO-001")) return 1;
        if (salesOrderId.equals("SO-005")) return 2;
        return 3;
    }

    private String mapStatus(String status) {
        if ("RELEASED".equals(status)) return "SCHEDULED";
        if ("DELAYED".equals(status)) return "DELAYED";
        return "PENDING";
    }

    private String formatDateTime(LocalDateTime dt) {
        return dt == null ? null : dt.format(ISO_FORMATTER);
    }

    private LocalDateTime parseDateTime(String s) {
        return s == null ? null : LocalDateTime.parse(s, ISO_FORMATTER);
    }

    private String generateColor(int index) {
        String[] colors = {"#1976d2", "#388e3c", "#f57c00", "#7b1fa2", "#00796b", "#c62828", "#0288d1", "#558b2f"};
        return colors[(index - 1) % colors.length];
    }
}