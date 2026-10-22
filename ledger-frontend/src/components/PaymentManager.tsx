import React, { useState } from "react";
import { createPayment, getPayment } from "../api";
import type{ Payment } from "../types";

interface Props {
  onPaymentProcessed?: () => void;
}

export function PaymentManager({ onPaymentProcessed }: Props) {
  const [dollars, setDollars] = useState("50.00");
  const [idempotencyKey, setIdempotencyKey] = useState(
    `order-${Date.now().toString().slice(-6)}`,
  );
  const [loading, setLoading] = useState(false);
  const [currency, setCurrency] = useState("USD");
  const [recentPayments, setRecentPayments] = useState<Payment[]>([]);
  const [error, setError] = useState<string | null>(null);

  const generateNewKey = () =>
    setIdempotencyKey(`order-${Date.now().toString().slice(-6)}`);
  const pollPaymentStatus = (id: string) => {
    let attempt = 0;
    const interval = setInterval(async () => {
      attempt++;
      try {
        const updated = await getPayment(id);
        setRecentPayments((prev) =>
          prev.map((p) => (p.id === id ? updated : p)),
        );
        if (updated.status != "PENDING" || attempt >= 8) {
          clearInterval(interval);
          onPaymentProcessed?.();
        }
      } catch {
        clearInterval(interval);
      }
    }, 1000);
  };

  const handleCreatePayment = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    const amountMinor = Math.round(parseFloat(dollars) * 100);
    if (isNaN(amountMinor) || amountMinor <= 0) {
      setError("Please enter a valid amount.");
      setLoading(false);
      return;
    }

    try {
      const created = await createPayment({
        idempotencyKey,
        amountMinor,
        currency,
      });
      setRecentPayments((prev) => [created, ...prev]);
      generateNewKey();
      pollPaymentStatus(created.id);
    } catch (err: any) {
      setError(err.message || "Error creating payment");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card">
      <h2>1.Payment Service(Port 8080)</h2>
      <form onSubmit={handleCreatePayment} className="form-grid">
        <div>
          <label>Amount (USD):</label>
          <input
            type="number"
            step="0.01"
            min="0.01"
            value={dollars}
            onChange={(e) => setDollars(e.target.value)}
            required
          />
        </div>

        <div>
          <label>Idempoency Key:</label>
          <div className="input-group">
            <input
              type="text"
              value={idempotencyKey}
              onChange={(e) => setIdempotencyKey(e.target.value)}
              required
            />
            <button
              type="button"
              onClick={generateNewKey}
              className="btn-secondary"
            >
              Random
            </button>
          </div>
        </div>

        <div className="preset-buttons">
          <small>Presets:</small>
          <button
            type="button"
            onClick={() => setDollars("50.00")}
            className="btn-chip"
          >
            %50 (Normal)
          </button>
          <button
            type="button"
            onClick={() => setDollars("15000.00")}
            className="btn-chip"
          >
            $15,000 (&gt; Limit)
          </button>
        </div>

        <button type="submit" disabled={loading} className="btn-primary">
          {loading ? "Submitting..." : "Create Payment"}
        </button>
      </form>

      {error && <div className="error-box">{error}</div>}
      <h3>Recent Submissions (Live Status)</h3>
      {recentPayments.length == 0 ? (
        <p className="muted">No payments created in this session</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Status</th>
              <th>Amount</th>
              <th>Idempotency Key</th>
              <th>Payment ID</th>
            </tr>
          </thead>
          <tbody>
            {recentPayments.map((p) => (
              <tr key={p.id}>
                <td>
                  <span
                    className={`status-badge status-${p.status.toLowerCase()}`}
                  >
                    {p.status}
                  </span>
                </td>
                <td>
                  ${(p.amountMinor / 100).toFixed(2)} {p.currency}
                </td>
                <td>
                  <code>{p.idempotencyKey}</code>
                </td>
                <td>
                  <small>{p.id}</small>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
