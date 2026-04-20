import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { ScheduleService } from '../../services/schedule.service';
import { StepExecution } from '../../models/order.model';

@Component({
  selector: 'app-line-schedule',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatFormFieldModule,
    MatSelectModule
  ],
  templateUrl: './line-schedule.component.html',
  styleUrls: ['./line-schedule.component.css']
})
export class LineScheduleComponent implements OnInit {
  isLoading = true;
  errorMsg = '';
  lines: string[] = [];
  selectedLine = '';
  lineSteps: StepExecution[] = [];
  displayedColumns = ['orderId', 'stepSeq', 'processName', 'plannedStart', 'plannedEnd', 'duration', 'delayed'];

  constructor(private scheduleService: ScheduleService) {}

  ngOnInit() {
    this.loadLines();
  }

  loadLines() {
    this.isLoading = true;
    this.scheduleService.getLinesSummary().subscribe({
      next: (summary) => {
        this.lines = Object.keys(summary.stepCountPerLine || {});
        if (this.lines.length) {
          this.selectedLine = this.lines[0];
          this.loadLineSchedule();
        } else {
          this.isLoading = false;
        }
      },
      error: (err) => {
        this.errorMsg = err.message;
        this.isLoading = false;
      }
    });
  }

  loadLineSchedule() {
    if (!this.selectedLine) return;
    this.isLoading = true;
    this.scheduleService.getLineSchedule(this.selectedLine).subscribe({
      next: (steps) => {
        this.lineSteps = steps;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMsg = err.message;
        this.isLoading = false;
      }
    });
  }

  formatDate(d: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleString();
  }

  formatDuration(min: number): string {
    const h = Math.floor(min / 60);
    const m = min % 60;
    return h > 0 ? `${h}h ${m > 0 ? m + 'm' : ''}` : `${m}m`;
  }
}