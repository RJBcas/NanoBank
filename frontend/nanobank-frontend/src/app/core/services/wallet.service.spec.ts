import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { WalletService } from './wallet.service';
import { Wallet } from '@core/models/wallet.model';

const mockWallet: Wallet = {
  id: 'w-1', userId: 'u-1', name: 'Savings', category: 'SAVINGS',
  balance: 1500, currency: 'COP', status: 'ACTIVE',
  createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z'
};

describe('WalletService', () => {
  let service: WalletService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(WalletService);
    http    = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => expect(service).toBeTruthy());

  it('getAll() should call GET /wallets', () => {
    service.getAll().subscribe(wallets => {
      expect(wallets.length).toBe(1);
      expect(wallets[0].name).toBe('Savings');
    });

    const req = http.expectOne('/api/v1/wallets');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [mockWallet] });
  });

  it('getAll() with category filter should include query param', () => {
    service.getAll('SAVINGS').subscribe();
    const req = http.expectOne(r => r.url.includes('/wallets') && r.params.get('category') === 'SAVINGS');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [mockWallet] });
  });

  it('create() should POST and return wallet', () => {
    service.create({ name: 'Savings', category: 'SAVINGS' }).subscribe(w => {
      expect(w.id).toBe('w-1');
    });

    const req = http.expectOne('/api/v1/wallets');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.name).toBe('Savings');
    req.flush({ success: true, data: mockWallet });
  });

  it('update() should PUT wallet', () => {
    service.update('w-1', { name: 'Updated' }).subscribe(w => {
      expect(w.name).toBe('Savings');
    });

    const req = http.expectOne('/api/v1/wallets/w-1');
    expect(req.request.method).toBe('PUT');
    req.flush({ success: true, data: mockWallet });
  });

  it('deactivate() should DELETE wallet', () => {
    service.deactivate('w-1').subscribe();
    const req = http.expectOne('/api/v1/wallets/w-1');
    expect(req.request.method).toBe('DELETE');
    req.flush({ success: true, data: null });
  });

  it('transfer() should POST to /wallets/{id}/transfer', () => {
    service.transfer('w-1', { destinationWalletId: 'w-2', amount: 500 }).subscribe();
    const req = http.expectOne('/api/v1/wallets/w-1/transfer');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.amount).toBe(500);
    req.flush({ success: true, data: null });
  });
});
