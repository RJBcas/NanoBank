import { Component, Input, Output, EventEmitter, inject, signal, computed, OnInit, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { CdkDragDrop, DragDropModule, transferArrayItem } from '@angular/cdk/drag-drop';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { Wallet } from '@core/models/wallet.model';
import { Transaction, TransactionFilter } from '@core/models/transaction.model';
import { WalletService } from '@core/services/wallet.service';
import { TransactionService } from '@core/services/transaction.service';
import { TransactionFormComponent } from '../transaction-form/transaction-form.component';
import { TransactionFilterComponent } from '../transaction-filter/transaction-filter.component';

@Component({
  selector: 'app-wallet-card',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DragDropModule, TransactionFormComponent, TransactionFilterComponent],
  templateUrl: './wallet-card.component.html'
})
export class WalletCardComponent implements OnInit, OnChanges {
  @Input({ required: true }) wallet!: Wallet;
  @Input() allWallets: Wallet[] = [];

  @Output() updated       = new EventEmitter<Wallet>();
  @Output() deleted       = new EventEmitter<string>();
  @Output() balanceChanged = new EventEmitter<void>();

  private walletSvc      = inject(WalletService);
  private transactionSvc = inject(TransactionService);
  private fb             = inject(FormBuilder);

  transactions     = signal<Transaction[]>([]);
  loadingTx        = signal(false);
  showAddTx        = signal(false);
  showEditWallet   = signal(false);
  filter           = signal<TransactionFilter>({});
  private filter$  = new Subject<TransactionFilter>();

  editForm = this.fb.group({
    name:        ['', [Validators.required]],
    description: ['']
  });

  otherWalletIds = computed(() =>
    this.allWallets.filter(w => w.id !== this.wallet.id && w.status === 'ACTIVE').map(w => w.id)
  );

  ngOnInit(): void {
    this.loadTransactions();
    this.filter$.pipe(debounceTime(300), distinctUntilChanged(
      (a, b) => JSON.stringify(a) === JSON.stringify(b)
    )).subscribe(f => { this.filter.set(f); this.loadTransactions(); });
  }

  ngOnChanges(): void {
    this.editForm.patchValue({ name: this.wallet.name, description: this.wallet.description ?? '' });
  }

  loadTransactions(): void {
    this.loadingTx.set(true);
    this.transactionSvc.getByWallet(this.wallet.id, this.filter()).subscribe({
      next: (page) => { this.transactions.set(page.content); this.loadingTx.set(false); },
      error: ()     => this.loadingTx.set(false)
    });
  }

  onFilterChanged(f: TransactionFilter): void {
    this.filter$.next(f);
  }

  onTransactionCreated(): void {
    this.showAddTx.set(false);
    this.loadTransactions();
    this.balanceChanged.emit();
  }

  // ── Drag & Drop ────────────────────────────────────────────
  onDrop(event: CdkDragDrop<Transaction[]>): void {
    if (event.previousContainer === event.container) return;

    const tx = event.previousContainer.data[event.previousIndex];
    this.transactionSvc.move(tx.id, { destinationWalletId: this.wallet.id }).subscribe({
      next: () => {
        transferArrayItem(
          event.previousContainer.data,
          event.container.data,
          event.previousIndex,
          event.currentIndex
        );
        this.balanceChanged.emit();
      }
    });
  }

  saveEdit(): void {
    if (this.editForm.invalid) return;
    this.walletSvc.update(this.wallet.id, this.editForm.value as any).subscribe({
      next: (w) => { this.updated.emit(w); this.showEditWallet.set(false); }
    });
  }

  deleteWallet(): void {
    if (!confirm(`Delete "${this.wallet.name}"? Balance must be zero.`)) return;
    this.walletSvc.deactivate(this.wallet.id).subscribe({
      next: () => this.deleted.emit(this.wallet.id)
    });
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency', currency: this.wallet.currency, maximumFractionDigits: 0
    }).format(amount);
  }

  categoryLabel(c: string): string {
    return { SAVINGS:'Savings', EXPENSES:'Expenses', INVESTMENTS:'Investments', CUSTOM:'Custom' }[c] ?? c;
  }

  txSign(type: string): string {
    return (type === 'INCOME' || type === 'TRANSFER_IN') ? '+' : '-';
  }

  txClass(type: string): string {
    return (type === 'INCOME' || type === 'TRANSFER_IN') ? 'amount-positive' : 'amount-negative';
  }
}
