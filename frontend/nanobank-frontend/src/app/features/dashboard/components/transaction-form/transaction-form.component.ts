import { Component, Input, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { TransactionService } from '@core/services/transaction.service';
import { TransactionType } from '@core/models/transaction.model';

@Component({
  selector: 'app-transaction-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transaction-form.component.html'
})
export class TransactionFormComponent {
  @Input({ required: true }) walletId!: string;
  @Input() currency = 'COP';
  @Output() created   = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  private txSvc = inject(TransactionService);
  private fb    = inject(FormBuilder);

  loading = signal(false);
  error   = signal('');

  types: TransactionType[] = ['INCOME', 'EXPENSE'];

  form = this.fb.group({
    type:        ['INCOME' as TransactionType, Validators.required],
    amount:      [null as number | null, [Validators.required, Validators.min(0.01)]],
    category:    [''],
    description: [''],
    occurredAt:  [new Date().toISOString().split('T')[0]]
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.error.set('');

    const val = this.form.value;
    this.txSvc.create({
      walletId:    this.walletId,
      type:        val.type!,
      amount:      val.amount!,
      currency:    this.currency,
      category:    val.category || undefined,
      description: val.description || undefined,
      occurredAt:  val.occurredAt || undefined
    }).subscribe({
      next: () => { this.loading.set(false); this.created.emit(); },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Failed to create transaction');
        this.loading.set(false);
      }
    });
  }
}
