import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TransactionService } from './transaction.service';
import { Transaction, PagedResponse } from '@core/models/transaction.model';

const mockTx: Transaction = {
  id: 'tx-1', walletId: 'w-1', userId: 'u-1',
  type: 'INCOME', amount: 500, currency: 'COP',
  category: 'SALARY', description: 'Monthly',
  occurredAt: '2026-06-01', createdAt: '2026-06-01T00:00:00Z'
};

const mockPage: PagedResponse<Transaction> = {
  content: [mockTx], page: 0, size: 20,
  totalElements: 1, totalPages: 1, last: true
};

describe('TransactionService', () => {
  let service: TransactionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(TransactionService);
    http    = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => expect(service).toBeTruthy());

  it('getByWallet() should GET transactions for wallet', () => {
    service.getByWallet('w-1').subscribe(page => {
      expect(page.content.length).toBe(1);
      expect(page.content[0].type).toBe('INCOME');
    });

    const req = http.expectOne(r => r.url.includes('/transactions/wallet/w-1'));
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: mockPage });
  });

  it('getByWallet() with type filter should include query param', () => {
    service.getByWallet('w-1', { type: 'INCOME' }).subscribe();
    const req = http.expectOne(r =>
      r.url.includes('/transactions/wallet/w-1') && r.params.get('type') === 'INCOME'
    );
    req.flush({ success: true, data: mockPage });
  });

  it('getByWallet() with date filter should include dateFrom and dateTo', () => {
    service.getByWallet('w-1', { dateFrom: '2026-01-01', dateTo: '2026-06-30' }).subscribe();
    const req = http.expectOne(r =>
      r.params.get('dateFrom') === '2026-01-01' && r.params.get('dateTo') === '2026-06-30'
    );
    req.flush({ success: true, data: mockPage });
  });

  it('create() should POST transaction', () => {
    service.create({ walletId: 'w-1', type: 'INCOME', amount: 500 }).subscribe(tx => {
      expect(tx.id).toBe('tx-1');
    });

    const req = http.expectOne('/api/v1/transactions');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.amount).toBe(500);
    req.flush({ success: true, data: mockTx });
  });

  it('move() should PATCH transaction with destination wallet', () => {
    service.move('tx-1', { destinationWalletId: 'w-2' }).subscribe(tx => {
      expect(tx.id).toBe('tx-1');
    });

    const req = http.expectOne('/api/v1/transactions/tx-1/move');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.destinationWalletId).toBe('w-2');
    req.flush({ success: true, data: mockTx });
  });
});
