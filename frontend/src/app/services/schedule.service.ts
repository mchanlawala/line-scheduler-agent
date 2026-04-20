import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ScheduleResult, Order, StepExecution, DashboardData, BatchExecution } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class ScheduleService {
  private readonly api = '/api/schedule';

  constructor(private http: HttpClient) {}

  getResult(): Observable<ScheduleResult> {
    return this.http.get<ScheduleResult>(`${this.api}/result`).pipe(catchError(this.handleError));
  }

  getAllOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.api}/orders`).pipe(catchError(this.handleError));
  }

  getGanttData(orderId: string): Observable<StepExecution[]> {
    return this.http.get<StepExecution[]>(`${this.api}/gantt/${orderId}`).pipe(catchError(this.handleError));
  }

  getDashboard(): Observable<DashboardData> {
    return this.http.get<DashboardData>(`${this.api}/dashboard`).pipe(catchError(this.handleError));
  }

  getInsights(): Observable<{ insights: string }> {
    return this.http.get<{ insights: string }>(`${this.api}/insights`).pipe(catchError(this.handleError));
  }

  getLineSchedule(lineId: string): Observable<StepExecution[]> {
    return this.http.get<StepExecution[]>(`${this.api}/line/${lineId}`).pipe(catchError(this.handleError));
  }

  getLinesSummary(): Observable<any> {
    return this.http.get<any>(`${this.api}/lines/summary`).pipe(catchError(this.handleError));
  }

  getLineBatches(lineId: string): Observable<BatchExecution[]> {
    return this.http.get<BatchExecution[]>(`${this.api}/line/${lineId}/batches`).pipe(catchError(this.handleError));
  }

  getLinesBatchSummary(): Observable<any> {
    return this.http.get<any>(`${this.api}/lines/batches-summary`).pipe(catchError(this.handleError));
  }

  private handleError(err: HttpErrorResponse) {
    const msg = err.error?.error || err.message || 'Unknown error';
    return throwError(() => new Error(msg));
  }
}