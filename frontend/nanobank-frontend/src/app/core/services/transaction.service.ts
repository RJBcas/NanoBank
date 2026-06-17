import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '@env/environment';
import {
  Transaction, CreateTransactionRequest,
  TransactionFilter, PagedResponse, MoveTransactionRequest
} from '@core/models/transaction.model';

interface ApiResponse<T> { success: boolean; data: T; message: string; }

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private http = inject(HttpClient);
  private base  = `${environment.apiUrl}/transactions`;

  getByWallet(
    walletId: string,
    filter: TransactionFilter = {},
    page = 0,
    size = 50
  ): Observable<PagedResponse<Transaction>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filter.type)     params = params.set('type', filter.type);
    if (filter.category) params = params.set('category', filter.category);
    if (filter.dateFrom) params = params.set('dateFrom', filter.dateFrom);
    if (filter.dateTo)   params = params.set('dateTo', filter.dateTo);

    return this.http
      .get<ApiResponse<PagedResponse<Transaction>>>(`${this.base}/wallet/${walletId}`, { params })
      .pipe(map(r => r.data));
  }

  create(req: CreateTransactionRequest): Observable<Transaction> {
    return this.http.post<ApiResponse<Transaction>>(this.base, req)
      .pipe(map(r => r.data));
  }

  move(transactionId: string, req: MoveTransactionRequest): Observable<Transaction> {
    return this.http.patch<ApiResponse<Transaction>>(`${this.base}/${transactionId}/move`, req)
      .pipe(map(r => r.data));
  }
}
