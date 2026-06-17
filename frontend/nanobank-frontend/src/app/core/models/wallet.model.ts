export type WalletCategory = 'SAVINGS' | 'EXPENSES' | 'INVESTMENTS' | 'CUSTOM';
export type WalletStatus   = 'ACTIVE' | 'INACTIVE';

export interface Wallet {
  id: string;
  userId: string;
  name: string;
  category: WalletCategory;
  balance: number;
  currency: string;
  status: WalletStatus;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateWalletRequest {
  name: string;
  category: WalletCategory;
  initialBalance?: number;
  currency?: string;
  description?: string;
}

export interface UpdateWalletRequest {
  name?: string;
  category?: WalletCategory;
  description?: string;
}

export interface WalletTransferRequest {
  destinationWalletId: string;
  amount: number;
}
