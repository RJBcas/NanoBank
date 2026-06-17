import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '@env/environment';
import {
  Wallet, CreateWalletRequest, UpdateWalletRequest,
  WalletCategory, WalletTransferRequest
} from '@core/models/wallet.model';

interface ApiResponse<T> { success: boolean; data: T; message: string; }

@Injectable({ providedIn: 'root' })
export class WalletService {
  private http = inject(HttpClient);
  private base  = `${environment.apiUrl}/wallets`;

  getAll(category?: WalletCategory): Observable<Wallet[]> {
    let params = new HttpParams();
    if (category) params = params.set('category', category);
    return this.http.get<ApiResponse<Wallet[]>>(this.base, { params })
      .pipe(map(r => r.data));
  }

  getById(id: string): Observable<Wallet> {
    return this.http.get<ApiResponse<Wallet>>(`${this.base}/${id}`)
      .pipe(map(r => r.data));
  }

  create(req: CreateWalletRequest): Observable<Wallet> {
    return this.http.post<ApiResponse<Wallet>>(this.base, req)
      .pipe(map(r => r.data));
  }

  update(id: string, req: UpdateWalletRequest): Observable<Wallet> {
    return this.http.put<ApiResponse<Wallet>>(`${this.base}/${id}`, req)
      .pipe(map(r => r.data));
  }

  deactivate(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`)
      .pipe(map(() => undefined));
  }

  transfer(sourceId: string, req: WalletTransferRequest): Observable<void> {
    return this.http.post<ApiResponse<void>>(`${this.base}/${sourceId}/transfer`, req)
      .pipe(map(() => undefined));
  }
}
