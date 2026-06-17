import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@core/services/auth.service';
import { WalletService } from '@core/services/wallet.service';
import { Wallet } from '@core/models/wallet.model';
import { WalletCardComponent } from './components/wallet-card/wallet-card.component';
import { WalletFormComponent } from './components/wallet-form/wallet-form.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, WalletCardComponent, WalletFormComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  private auth          = inject(AuthService);
  private walletService = inject(WalletService);

  wallets      = signal<Wallet[]>([]);
  loading      = signal(true);
  showForm     = signal(false);
  selectedWallet = signal<Wallet | null>(null);

  currentUser  = this.auth.currentUser;

  totalBalance = computed(() =>
    this.wallets().reduce((sum, w) => sum + w.balance, 0)
  );

  activeWallets = computed(() =>
    this.wallets().filter(w => w.status === 'ACTIVE').length
  );

  ngOnInit(): void {
    this.loadWallets();
  }

  loadWallets(): void {
    this.loading.set(true);
    this.walletService.getAll().subscribe({
      next: (wallets) => { this.wallets.set(wallets); this.loading.set(false); },
      error: ()        => this.loading.set(false)
    });
  }

  onWalletCreated(wallet: Wallet): void {
    this.wallets.update(list => [wallet, ...list]);
    this.showForm.set(false);
  }

  onWalletUpdated(updated: Wallet): void {
    this.wallets.update(list => list.map(w => w.id === updated.id ? updated : w));
  }

  onWalletDeleted(id: string): void {
    this.wallets.update(list => list.filter(w => w.id !== id));
  }

  onWalletBalanceChanged(): void {
    this.loadWallets();
  }

  logout(): void {
    this.auth.logout();
  }

  formatCurrency(amount: number, currency = 'COP'): string {
    return new Intl.NumberFormat('es-CO', { style: 'currency', currency, maximumFractionDigits: 0 })
      .format(amount);
  }
}
