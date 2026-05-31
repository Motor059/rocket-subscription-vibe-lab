export type SubscriptionStatus = 'DETECTED' | 'CONFIRMED' | 'WARNING' | 'CANCELED' | 'IGNORED';

export interface Subscription {
  id: number;
  merchantName: string;
  amount: number;
  status: SubscriptionStatus;
  lastTransactionDate?: string;
}