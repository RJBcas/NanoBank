import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Wallet } from '@core/models/wallet.model';
import { AuthService } from '@core/services/auth.service';
import { signal } from '@angular/core';

const mockWallets: Wallet[] = [
  { id: 'w-1', userId: 'u-1', name: 'Savings', category: 'SAVINGS', balance: 1000, currency: 'COP', status: 'ACTIVE', createdAt: '', updatedAt: '' },
  { id: 'w-2', userId: 'u-1', name: 'Expenses', category: 'EXPENSES', balance: 500, currency: 'COP', status: 'ACTIVE', createdAt: '', updatedAt: '' }
];

describe('DashboardComponent', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            currentUser: signal({ fullName: 'Test User', email: 'test@test.com' }),
            isLoggedIn:  signal(true),
            logout: jasmine.createSpy('logout')
          }
        }
      ]
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should create', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    http.expectOne(r => r.url.includes('/wallets')).flush({ success: true, data: [] });
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should compute totalBalance from wallets signal', fakeAsync(() => {
    const fixture = TestBed.createComponent(DashboardComponent);
    const comp    = fixture.componentInstance;
    fixture.detectChanges();

    http.expectOne(r => r.url.includes('/wallets')).flush({ success: true, data: mockWallets });
    tick();

    expect(comp.totalBalance()).toBe(1500);
  }));

  it('should compute activeWallets count', fakeAsync(() => {
    const fixture = TestBed.createComponent(DashboardComponent);
    const comp    = fixture.componentInstance;
    fixture.detectChanges();

    http.expectOne(r => r.url.includes('/wallets')).flush({ success: true, data: mockWallets });
    tick();

    expect(comp.activeWallets()).toBe(2);
  }));

  it('onWalletCreated() should add wallet to list', fakeAsync(() => {
    const fixture = TestBed.createComponent(DashboardComponent);
    const comp    = fixture.componentInstance;
    fixture.detectChanges();

    http.expectOne(r => r.url.includes('/wallets')).flush({ success: true, data: [] });
    tick();

    const newWallet = mockWallets[0];
    comp.onWalletCreated(newWallet);

    expect(comp.wallets().length).toBe(1);
    expect(comp.wallets()[0].name).toBe('Savings');
  }));

  it('onWalletDeleted() should remove wallet from list', fakeAsync(() => {
    const fixture = TestBed.createComponent(DashboardComponent);
    const comp    = fixture.componentInstance;
    fixture.detectChanges();

    http.expectOne(r => r.url.includes('/wallets')).flush({ success: true, data: mockWallets });
    tick();

    comp.onWalletDeleted('w-1');
    expect(comp.wallets().length).toBe(1);
    expect(comp.wallets()[0].id).toBe('w-2');
  }));

  it('formatCurrency() should format COP correctly', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    http.expectOne(r => r.url.includes('/wallets')).flush({ success: true, data: [] });

    const result = fixture.componentInstance.formatCurrency(1000000);
    expect(result).toContain('1');
  });
});
