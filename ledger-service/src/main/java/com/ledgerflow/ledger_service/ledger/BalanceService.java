package com.ledgerflow.ledger_service.ledger;

import com.ledgerflow.ledger_service.ledger.dto.AccountBalanceResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Balances are derived from ledger_entries on every cache miss, applying each
 * account's normal-balance side (debit-normal for ASSET/EXPENSE, credit-normal
 * otherwise), then cached in Redis for CACHE_TTL and evicted on write.
 */
@Service
public class BalanceService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(30);
    private static final String CACHE_KEY_PREFIX = "ledger:balance:";
    private static final Set<String> DEBIT_NORMAL_TYPES = Set.of("ASSET", "EXPENSE");

    private final LedgerAccountRepository ledgerAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final StringRedisTemplate redisTemplate;

    public BalanceService(LedgerAccountRepository ledgerAccountRepository,
                          LedgerEntryRepository ledgerEntryRepository,
                          StringRedisTemplate redisTemplate) {
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.redisTemplate = redisTemplate;
    }

    // Entry point used by AccountController. Looks up the account by its code
    // (e.g. "PSP_RECEIVABLE"), then resolves its balance via cache-or-compute.
    // Returns empty if no account with that code exists.
    public Optional<AccountBalanceResponse> getBalance(String accountCode) {
        return ledgerAccountRepository.findByAccountCode(accountCode)
                .map(this::resolveBalance);
    }

    // Called by PaymentEventProcessor right after it books new ledger_entries,
    // so the next read recomputes instead of serving a stale cached number.
    public void evict(String accountCode) {
        redisTemplate.delete(cacheKey(accountCode));
    }

    // Cache-aside: try Redis first (fast path), and only hit Postgres to
    // recompute the balance on a miss, then repopulate the cache.
    private AccountBalanceResponse resolveBalance(LedgerAccount account) {
        String cached = redisTemplate.opsForValue().get(cacheKey(account.getAccountCode()));
        if (cached != null) {
            // Cache hit: no DB query, just wrap the stored value in the response DTO.
            return new AccountBalanceResponse(account.getAccountCode(), account.getAccountType(),
                    account.getCurrency(), Long.parseLong(cached), true);
        }

        // Cache miss: compute from the ledger_entries table and cache the result
        // for CACHE_TTL so repeated reads within that window skip Postgres entirely.
        long balance = computeBalance(account);
        redisTemplate.opsForValue().set(cacheKey(account.getAccountCode()), Long.toString(balance), CACHE_TTL);
        return new AccountBalanceResponse(account.getAccountCode(), account.getAccountType(),
                account.getCurrency(), balance, false);
    }

    // Double-entry balance math: sum every DR entry and every CR entry posted
    // against this account, then net them according to the account's "normal
    // balance" side. ASSET/EXPENSE accounts grow on the debit side (e.g. more
    // money owed TO us increases PSP_RECEIVABLE), so balance = debits - credits.
    // LIABILITY/REVENUE/EQUITY accounts grow on the credit side (e.g. more
    // money owed BY us increases MERCHANT_PAYABLE), so balance = credits - debits.
    private long computeBalance(LedgerAccount account) {
        long debits = ledgerEntryRepository.sumByAccountIdAndDirection(account.getId(), EntryDirection.DR);
        long credits = ledgerEntryRepository.sumByAccountIdAndDirection(account.getId(), EntryDirection.CR);
        return DEBIT_NORMAL_TYPES.contains(account.getAccountType()) ? debits - credits : credits - debits;
    }

    // Redis key an account's cached balance is stored under, e.g.
    // "ledger:balance:PSP_RECEIVABLE".
    private String cacheKey(String accountCode) {
        return CACHE_KEY_PREFIX + accountCode;
    }
}
