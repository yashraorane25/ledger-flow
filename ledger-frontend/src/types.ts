export interface CreatePaymentRequest {
  idempotencyKey: string;
  amountMinor: number;
  currency: string;
}

export interface Payment {
  id: string;
  idempotencyKey: string;
  amountMinor: number;
  currency: string;
  status: "PENDING" | "AUTHORIZED" | "FAILED";
  createdAt: string;
}

export interface LedgerAccount {
  id: string;
  accountCode: string;
  accountType: "ASSET" | "LIABILITY" | "REVENUE" | string;
  currency: string;
}

export interface AccountBalance {
  accountCode: string;
  accountType: string;
  currency: string;
  balanceMinor: number;
  cached: boolean;
}

export interface ReconciliationReport {
  id: string;
  runDate: string;
  paymentId: string | null;
  mismatchType: string;
  expectedMinor: number | null;
  actualMinor: number | null;
  createdAt: string;
}
