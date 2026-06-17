import { Component, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { WalletService } from '@core/services/wallet.service';
import { Wallet, WalletCategory } from '@core/models/wallet.model';

@Component({
  selector: 'app-wallet-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './wallet-form.component.html'
})
export class WalletFormComponent {
  @Output() created   = new EventEmitter<Wallet>();
  @Output() cancelled = new EventEmitter<void>();

  private walletSvc = inject(WalletService);
  private fb        = inject(FormBuilder);

  loading = signal(false);
  error   = signal('');

  categories: WalletCategory[] = ['SAVINGS', 'EXPENSES', 'INVESTMENTS', 'CUSTOM'];

  form = this.fb.group({
    name:           ['', [Validators.required, Validators.maxLength(100)]],
    category:       ['SAVINGS' as WalletCategory, Validators.required],
    initialBalance: [0, [Validators.min(0)]],
    currency:       ['COP'],
    description:    ['']
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.error.set('');

    this.walletSvc.create(this.form.value as any).subscribe({
      next: (wallet) => { this.loading.set(false); this.created.emit(wallet); },
      error: (err)   => {
        this.error.set(err.error?.message ?? 'Failed to create wallet');
        this.loading.set(false);
      }
    });
  }
}
