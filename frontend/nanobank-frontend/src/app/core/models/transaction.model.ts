export type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER_IN' | 'TRANSFER_OUT';

export interface Transaction {
  id: string;
  walletId: string;
  userId: string;
  type: TransactionType;
  amount: number;
  currency: string;
  category?: string;
  description?: string;
  occurredAt: string;
  createdAt: string;
}

export interface CreateTransactionRequest {
  walletId: string;
  type: TransactionType;
  amount: number;
  currency?: string;
  category?: string;
  description?: string;
  occurredAt?: string;
}

export interface TransactionFilter {
  type?: TransactionType | '';
  category?: string;
  dateFrom?: string;
  dateTo?: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface MoveTransactionRequest {
  destinationWalletId: string;
}
