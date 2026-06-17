import { Component, Output, EventEmitter, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { TransactionFilter } from '@core/models/transaction.model';

@Component({
  selector: 'app-transaction-filter',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transaction-filter.component.html'
})
export class TransactionFilterComponent implements OnInit {
  @Output() filterChanged = new EventEmitter<TransactionFilter>();

  private fb = inject(FormBuilder);

  form = this.fb.group({
    type:     [''],
    category: [''],
    dateFrom: [''],
    dateTo:   ['']
  });

  ngOnInit(): void {
    // Real-time filter: emit on every change with debounce
    this.form.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b))
    ).subscribe(val => {
      const filter: TransactionFilter = {};
      if (val.type)     filter.type     = val.type as any;
      if (val.category) filter.category = val.category;
      if (val.dateFrom) filter.dateFrom = val.dateFrom;
      if (val.dateTo)   filter.dateTo   = val.dateTo;
      this.filterChanged.emit(filter);
    });
  }

  reset(): void {
    this.form.reset({ type: '', category: '', dateFrom: '', dateTo: '' });
  }
}
