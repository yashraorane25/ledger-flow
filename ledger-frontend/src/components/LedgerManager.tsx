import { useEffect, useState } from "react";
import { getAccounts, getAccountBalance } from "../api";
import type { LedgerAccount, AccountBalance } from "../types";

export function LedgerManager({ refreshKey }: { refreshKey: number }) {
  const [accounts, setAccounts] = useState<LedgerAccount[]>([]);
  const [balances, setBalances] = useState<Record<string, AccountBalance>>({});
  const [loading, setLoading] = useState(false);

  const fetchAllBalances = async () => {
    setLoading(true);
    try {
      const accts = await getAccounts();
      setAccounts(accts);

      const balanceMap: Record<string, AccountBalance> = {};
      for (const acct of accts) {
        balanceMap[acct.accountCode] = await getAccountBalance(
          acct.accountCode,
        );
      }
      setBalances(balanceMap);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAllBalances();
  }, [refreshKey]);

  return (
    <div className="card">
      <div className="card-header">
        <h2>2. Double-Entry Ledger & Redis Balances (Port 8081)</h2>
        <button
          onClick={fetchAllBalances}
          disabled={loading}
          className="btn-secondary"
        >
          {loading ? "Refreshing..." : "🔄 Refresh Balances"}
        </button>
      </div>

      <div className="grid-accounts">
        {accounts.map((acct) => {
          const bal = balances[acct.accountCode];
          return (
            <div key={acct.id} className="account-card">
              <div className="account-top">
                <span className="account-code">{acct.accountCode}</span>
                <span className="account-type-badge">{acct.accountType}</span>
              </div>

              <div className="balance-amount">
                {/* {bal ? `$${(bal.balanceMinor / 100).toFixed(2)}` : "..."} */}
                {bal
                  ? `$${(bal.balanceMinor / 100).toLocaleString("en-US", {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}`
                  : "..."}
                <span className="currency">{acct.currency}</span>
              </div>

              <div className="cache-status">
                {bal ? (
                  bal.cached ? (
                    <span className="badge-cache-hit">
                      ⚡ Redis Cache (Hit)
                    </span>
                  ) : (
                    <span className="badge-cache-miss">
                      🐘 Postgres (Cache Miss)
                    </span>
                  )
                ) : null}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
