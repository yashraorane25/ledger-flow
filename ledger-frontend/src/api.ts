import type{
  CreatePaymentRequest,
  Payment,
  LedgerAccount,
  AccountBalance,
  ReconciliationReport,
} from "./types";

export async function createPayment(
  req: CreatePaymentRequest,
): Promise<Payment> {
  const res = await fetch("/api/payments", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  if (!res.ok) throw new Error(`Failed to create payment: ${res.statusText}`);
  return res.json();
}

export async function getPayment(id: string): Promise<Payment> {
  const res = await fetch(`/api/payments/${id}`);
  if (!res.ok) throw new Error(`Payment not found: ${res.statusText}`);
  return res.json();
}

// Ledger Service APIs
export async function getAccounts(): Promise<LedgerAccount[]> {
  const res = await fetch("/api/ledger/accounts");
  if (!res.ok) throw new Error(`Failed to fetch accounts: ${res.statusText}`);
  return res.json();
}

export async function getAccountBalance(
  accountCode: string,
): Promise<AccountBalance> {
  const res = await fetch(`/api/ledger/accounts/${accountCode}/balance`);
  if (!res.ok) throw new Error(`Failed to fetch balance: ${res.statusText}`);
  return res.json();
}

export async function triggerReconciliation(): Promise<ReconciliationReport[]> {
  const res = await fetch("/api/ledger/reconciliation/run", { method: "POST" });
  if (!res.ok)
    throw new Error(`Failed to run reconciliation: ${res.statusText}`);
  return res.json();
}

export async function getReconciliationReports(): Promise<
  ReconciliationReport[]
> {
  const res = await fetch("/api/ledger/reconciliation/reports");
  if (!res.ok) throw new Error(`Failed to fetch reports: ${res.statusText}`);
  return res.json();
}
