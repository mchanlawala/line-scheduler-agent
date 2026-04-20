import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { ScheduleService } from '../../services/schedule.service';
import { Order, StepExecution } from '../../models/order.model';

interface GanttBar {
  stepSeq: number;
  processName: string;
  lineId: string;
  start: Date;
  end: Date;
  durationMin: number;
  isDelayed: boolean;
  delayReason: string | null;
  color: string;
  leftPct: number;
  widthPct: number;
}

interface GanttOrder {
  order: Order;
  bars: GanttBar[];
  totalHours: number;
}

@Component({
  selector: 'app-gantt-chart',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatSelectModule
  ],
  templateUrl: './gantt-chart.component.html',
  styleUrls: ['./gantt-chart.component.css']
})
export class GanttChartComponent implements OnInit {
  isLoading = true;
  errorMsg = '';

  allOrders: Order[] = [];
  selectedOrderId = '';
  selectedOrder: Order | null = null;

  viewMode: 'all' | 'single' = 'all';
  filterStatus = 'ALL';

  // All orders Gantt
  allGantts: GanttOrder[] = [];
  allMinDate: Date = new Date();
  allMaxDate: Date = new Date();
  allTimeMarkers: { label: string; pct: number }[] = [];

  // Single order Gantt
  singleGantt: GanttBar[] = [];
  singleMinDate: Date = new Date();
  singleMaxDate: Date = new Date();
  singleTimeMarkers: { label: string; pct: number }[] = [];

  tooltip = { visible: false, x: 0, y: 0, name: '', line: '', start: '', end: '', duration: '', delayed: false, delayReason: '' };

  private colors = ['#1976d2', '#388e3c', '#f57c00', '#7b1fa2', '#00796b', '#c62828', '#0288d1', '#558b2f'];

  constructor(private scheduleService: ScheduleService) {}

  ngOnInit() {
    this.loadOrders();
  }

  loadOrders() {
    this.isLoading = true;
    this.scheduleService.getAllOrders().subscribe({
      next: (orders) => {
        this.allOrders = orders;
        if (orders.length) {
          this.selectedOrderId = orders[0].orderId;
          this.selectedOrder = orders[0];
          this.buildAllGantts();
          this.buildSingleGantt();
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMsg = err.message;
        this.isLoading = false;
      }
    });
  }

  buildAllGantts() {
    if (!this.allOrders.length) return;
    const allStarts: number[] = [];
    const allEnds: number[] = [];
    for (const order of this.allOrders) {
      if (order.steps?.length) {
        allStarts.push(new Date(order.steps[0].plannedStart).getTime());
        allEnds.push(new Date(order.steps[order.steps.length-1].plannedEnd).getTime());
      }
    }
    const minT = Math.min(...allStarts);
    const maxT = Math.max(...allEnds);
    const span = maxT - minT || 1;
    this.allMinDate = new Date(minT);
    this.allMaxDate = new Date(maxT);
    this.allTimeMarkers = this.buildMarkers(minT, maxT, 6);

    this.allGantts = this.allOrders.map((order, idx) => {
      const bars: GanttBar[] = order.steps.map((step, sIdx) => {
        const start = new Date(step.plannedStart).getTime();
        const end = new Date(step.plannedEnd).getTime();
        return {
          stepSeq: step.stepSeq,
          processName: step.processName,
          lineId: step.lineId,
          start: new Date(step.plannedStart),
          end: new Date(step.plannedEnd),
          durationMin: step.durationMinutes,
          isDelayed: step.isDelayed,
          delayReason: step.delayReason,
          color: step.color || this.colors[sIdx % this.colors.length],
          leftPct: ((start - minT) / span) * 100,
          widthPct: Math.max(0.5, ((end - start) / span) * 100)
        };
      });
      const totalHours = order.totalLeadTimeMinutes / 60;
      return { order, bars, totalHours: Math.round(totalHours) };
    });
  }

  buildSingleGantt() {
    const order = this.allOrders.find(o => o.orderId === this.selectedOrderId);
    if (!order?.steps?.length) { this.singleGantt = []; return; }
    const steps = order.steps;
    const minT = new Date(steps[0].plannedStart).getTime();
    const maxT = new Date(steps[steps.length-1].plannedEnd).getTime();
    const span = maxT - minT || 1;
    this.singleMinDate = new Date(minT);
    this.singleMaxDate = new Date(maxT);
    this.singleTimeMarkers = this.buildMarkers(minT, maxT, 5);

    this.singleGantt = steps.map((step, idx) => {
      const start = new Date(step.plannedStart).getTime();
      const end = new Date(step.plannedEnd).getTime();
      return {
        stepSeq: step.stepSeq,
        processName: step.processName,
        lineId: step.lineId,
        start: new Date(step.plannedStart),
        end: new Date(step.plannedEnd),
        durationMin: step.durationMinutes,
        isDelayed: step.isDelayed,
        delayReason: step.delayReason,
        color: step.color || this.colors[idx % this.colors.length],
        leftPct: ((start - minT) / span) * 100,
        widthPct: Math.max(0.5, ((end - start) / span) * 100)
      };
    });
  }

  buildMarkers(minT: number, maxT: number, count: number): { label: string; pct: number }[] {
    const markers = [];
    for (let i = 0; i <= count; i++) {
      const t = new Date(minT + (maxT - minT) * i / count);
      markers.push({ label: this.formatTime(t), pct: (i / count) * 100 });
    }
    return markers;
  }

  onOrderSelect(orderId: string) {
    this.selectedOrderId = orderId;
    this.selectedOrder = this.allOrders.find(o => o.orderId === orderId) || null;
    this.buildSingleGantt();
    this.viewMode = 'single';
  }

  showTooltip(event: MouseEvent, bar: GanttBar) {
    this.tooltip = {
      visible: true,
      x: event.clientX + 14,
      y: event.clientY - 10,
      name: bar.processName,
      line: bar.lineId,
      start: this.formatDateTime(bar.start),
      end: this.formatDateTime(bar.end),
      duration: this.formatDuration(bar.durationMin),
      delayed: bar.isDelayed,
      delayReason: bar.delayReason || ''
    };
  }

  hideTooltip() { this.tooltip.visible = false; }

  formatDateTime(d: Date): string { return d.toLocaleString(); }
  formatTime(d: Date): string { return d.toLocaleTimeString([], { hour: '2-digit', minute:'2-digit' }); }
  formatDuration(min: number): string {
    const h = Math.floor(min / 60);
    const m = min % 60;
    return h > 0 ? `${h}h ${m > 0 ? m + 'm' : ''}` : `${m}m`;
  }

  statusClass(status: string): string {
    const map: Record<string, string> = { SCHEDULED: 'chip-blue', DELAYED: 'chip-red', COMPLETED: 'chip-green', PENDING: 'chip-orange' };
    return map[status] || 'chip-grey';
  }

  get uniqueStatuses(): string[] {
    const set = new Set(this.allOrders.map(o => o.status));
    return ['ALL', ...Array.from(set)];
  }

  get filteredOrders() {
    if (this.filterStatus === 'ALL') return this.allOrders;
    return this.allOrders.filter(o => o.status === this.filterStatus);
  }
}