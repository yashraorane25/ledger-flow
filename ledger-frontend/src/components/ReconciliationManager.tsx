import { useEffect, useState } from "react";
import { triggerReconciliation, getReconciliationReports } from "../api";
import type { ReconciliationReport } from "../types";

export function ReconciliationManager() {
  const [reports, setReports] = useState<ReconciliationReport[]>([]);
  const [loading, setLoading] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  const loadReports = async () => {
    try {
      const data = await getReconciliationReports();
      setReports(data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleRunReconcilation = async () => {
    setLoading(true);
    setStatusMessage(null);
    try {
      const results = await triggerReconciliation();
      if (results.length == 0) {
        setStatusMessage(
          "Reconciliation complete: All payments match ledger records!",
        );
      } else {
        setStatusMessage(`Found ${results.length} mismatch(es)`);
      }
      await loadReports();
    } catch (err: any) {
      setStatusMessage(`Error : ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReports();
  }, []);

  // Deduplicate by paymentId, keeping the most recent occurrence
  const uniqueReports = reports.filter(
    (report, index, self) =>
      index ===
      self.findIndex(
        (r) => (r.paymentId || r.id) === (report.paymentId || report.id)
      )
  );

  return (
    <div className="card">
      <div className="card-header">
        <h2>3. Reconciliation & Audit</h2>
        <button
          onClick={handleRunReconcilation}
          disabled={loading}
          className="btn-primary"
        >
          {loading ? "Running Audit..." : "▶ Run Audit Now"}
        </button>
      </div>

      {statusMessage && <div className="info-box">{statusMessage}</div>}

      <h3>Recent Mismatches</h3>
      {uniqueReports.length === 0 ? (
        <p className="muted">No mismatches detected. Ledgers are consistent.</p>
      ) : (
        <table className="table">
          <thead>
            <tr>
              <th>Run Date</th>
              <th>Mismatch Type</th>
              <th>Expected ($)</th>
              <th>Actual ($)</th>
              <th>Payment ID</th>
            </tr>
          </thead>
          <tbody>
            {uniqueReports.map((r) => (
              <tr key={r.id}>
                <td>{r.runDate}</td>
                <td>
                  <code>{r.mismatchType}</code>
                </td>
                <td>
                  {r.expectedMinor !== null
                    ? `$${(r.expectedMinor / 100).toFixed(2)}`
                    : "N/A"}
                </td>
                <td>
                  {r.actualMinor !== null
                    ? `$${(r.actualMinor / 100).toFixed(2)}`
                    : "N/A"}
                </td>
                <td>
                  <small>{r.paymentId || "N/A"}</small>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
