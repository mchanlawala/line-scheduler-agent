// src/app/pages/report/report.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { ScheduleService } from '../../services/schedule.service';
import { Order, BatchExecution } from '../../models/order.model';

interface SalesOrderSummary {
  salesOrderId: string;
  material: string;
  quantity: number;
  dueDate: string;
  status: string;
  productionOrders: string[];
}

interface ProductionOrderSummary {
  productionOrderId: string;
  salesOrderId: string;
  material: string;
  quantity: number;
  status: string;
  estimatedCompletion: string;
  totalLeadTimeHours: number;
}

interface BatchReport {
  orderId: string;
  salesOrderId: string;        // NEW
  stepSeq: number;
  processName: string;
  lineId: string;
  batchNumber: number;
  batchQuantity: number;
  plannedStart: string;
  plannedEnd: string;
  durationMinutes: number;
}

interface LineBatchSummary {
  lineId: string;
  totalBatches: number;
  totalQuantity: number;
  totalDurationHours: number;
}

@Component({
  selector: 'app-report',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatTabsModule,
    MatFormFieldModule,
    MatSelectModule
  ],
  templateUrl: './report.component.html',
  styleUrls: ['./report.component.css']
})
export class ReportComponent implements OnInit {
  isLoading = true;
  errorMsg = '';

  salesOrders: SalesOrderSummary[] = [];
  salesColumns = ['salesOrderId', 'material', 'quantity', 'dueDate', 'status', 'productionOrders'];

  prodOrders: ProductionOrderSummary[] = [];
  prodColumns = ['productionOrderId', 'salesOrderId', 'material', 'quantity', 'status', 'estimatedCompletion', 'leadTime'];

  batches: BatchReport[] = [];
  batchColumns = ['salesOrderId', 'orderId', 'stepSeq', 'processName', 'lineId', 'batchNumber', 'batchQuantity', 'plannedStart', 'plannedEnd', 'duration'];

  lineSummaries: LineBatchSummary[] = [];
  lineColumns = ['lineId', 'totalBatches', 'totalQuantity', 'totalDurationHours'];

  lines: string[] = [];
  selectedLine = 'ALL';

  constructor(private scheduleService: ScheduleService) {}

  ngOnInit() {
    this.loadReport();
  }

  loadReport() {
    this.isLoading = true;
    this.scheduleService.getResult().subscribe({
      next: (result) => {
        if (!result || !result.orders) {
          this.errorMsg = 'No data available. Please ensure production_orders.json is loaded.';
          this.isLoading = false;
          return;
        }
        this.buildSalesOrders(result.orders);
        this.buildProductionOrders(result.orders);
        this.buildBatchReport(result.orders);
        this.buildLineSummaries();
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMsg = err.message;
        this.isLoading = false;
      }
    });
  }

  buildSalesOrders(orders: Order[]) {
    const map = new Map<string, SalesOrderSummary>();
    for (const order of orders) {
      // Derive sales order ID from production order ID (since backend may not include it yet)
      const salesId = `SO-${order.orderId.slice(-3)}`;
      const existing = map.get(salesId);
      if (existing) {
        existing.quantity += order.quantity;
        existing.productionOrders.push(order.orderId);
      } else {
        map.set(salesId, {
          salesOrderId: salesId,
          material: order.panelType,
          quantity: order.quantity,
          dueDate: order.dueDate,
          status: order.status,
          productionOrders: [order.orderId]
        });
      }
    }
    this.salesOrders = Array.from(map.values());
  }

  buildProductionOrders(orders: Order[]) {
    this.prodOrders = orders.map(o => ({
      productionOrderId: o.orderId,
      salesOrderId: `SO-${o.orderId.slice(-3)}`,
      material: o.panelType,
      quantity: o.quantity,
      status: o.status,
      estimatedCompletion: o.estimatedCompletion,
      totalLeadTimeHours: Math.round(o.totalLeadTimeMinutes / 60)
    }));
  }

  buildBatchReport(orders: Order[]) {
    const allBatches: BatchReport[] = [];
    for (const order of orders) {
      const salesId = `SO-${order.orderId.slice(-3)}`;
      for (const step of order.steps) {
        if (step.batches && step.batches.length) {
          for (const batch of step.batches) {
            allBatches.push({
              orderId: batch.orderId,
              salesOrderId: salesId,
              stepSeq: batch.stepSeq,
              processName: batch.processName,
              lineId: batch.lineId,
              batchNumber: batch.batchNumber,
              batchQuantity: batch.batchQuantity,
              plannedStart: batch.plannedStart,
              plannedEnd: batch.plannedEnd,
              durationMinutes: batch.durationMinutes
            });
          }
        } else {
          // Single batch (no splitting)
          allBatches.push({
            orderId: order.orderId,
            salesOrderId: salesId,
            stepSeq: step.stepSeq,
            processName: step.processName,
            lineId: step.lineId,
            batchNumber: 1,
            batchQuantity: order.quantity,
            plannedStart: step.plannedStart,
            plannedEnd: step.plannedEnd,
            durationMinutes: step.durationMinutes
          });
        }
      }
    }
    this.batches = allBatches;
    this.lines = [...new Set(allBatches.map(b => b.lineId))];
  }

  buildLineSummaries() {
    const map = new Map<string, { batches: number; quantity: number; duration: number }>();
    for (const batch of this.batches) {
      const existing = map.get(batch.lineId) || { batches: 0, quantity: 0, duration: 0 };
      existing.batches += 1;
      existing.quantity += batch.batchQuantity;
      existing.duration += batch.durationMinutes;
      map.set(batch.lineId, existing);
    }
    this.lineSummaries = Array.from(map.entries()).map(([lineId, stats]) => ({
      lineId,
      totalBatches: stats.batches,
      totalQuantity: stats.quantity,
      totalDurationHours: Math.round(stats.duration / 60)
    }));
  }

  get filteredBatches(): BatchReport[] {
    if (this.selectedLine === 'ALL') return this.batches;
    return this.batches.filter(b => b.lineId === this.selectedLine);
  }

  // ========== Helper methods for template ==========
  statusClass(status: string): string {
    const map: Record<string, string> = {
      SCHEDULED: 'chip-blue',
      DELAYED: 'chip-red',
      COMPLETED: 'chip-green',
      PENDING: 'chip-orange'
    };
    return map[status] ?? 'chip-grey';
  }

  formatDate(d: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleString('en-GB', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  formatDuration(min: number): string {
    if (!min) return '—';
    const h = Math.floor(min / 60);
    const m = min % 60;
    return h > 0 ? `${h}h ${m > 0 ? m + 'm' : ''}`.trim() : `${m}m`;
  }
}