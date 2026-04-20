import { Routes } from '@angular/router';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { GanttChartComponent } from './pages/gantt-chart/gantt-chart.component';
import { LineScheduleComponent } from './pages/line-schedule/line-schedule.component';
import { ReportComponent } from './pages/report/report.component';   // ADD

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'gantt', component: GanttChartComponent },
  { path: 'line-schedule', component: LineScheduleComponent },
  { path: 'report', component: ReportComponent }   // ADD
];