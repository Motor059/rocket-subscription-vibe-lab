export type SubscriptionStatus = 'DETECTED' | 'WARNING' | 'CANCELED' | 'IGNORED';

export interface Subscription {
  id: number;
  merchantName: string;
  amount: number;
  status: SubscriptionStatus;
  lastTransactionDate?: string;
}

export interface ActionResponse {
  id: number;
  status: SubscriptionStatus;
}

export interface DetectResponse {
  message: string;
  detectedCount: number;
}