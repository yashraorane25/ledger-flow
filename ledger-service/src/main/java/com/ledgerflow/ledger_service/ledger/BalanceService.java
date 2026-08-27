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

    public Optional<AccountBalanceResponse> getBalance(String accountCode) {
        return ledgerAccountRepository.findByAccountCode(accountCode)
                .map(this::resolveBalance);
    }

    public void evict(String accountCode) {
        redisTemplate.delete(cacheKey(accountCode));
    }

    private AccountBalanceResponse resolveBalance(LedgerAccount account) {
        String cached = redisTemplate.opsForValue().get(cacheKey(account.getAccountCode()));
        if (cached != null) {
            return new AccountBalanceResponse(account.getAccountCode(), account.getAccountType(),
                    account.getCurrency(), Long.parseLong(cached), true);
        }

        long balance = computeBalance(account);
        redisTemplate.opsForValue().set(cacheKey(account.getAccountCode()), Long.toString(balance), CACHE_TTL);
        return new AccountBalanceResponse(account.getAccountCode(), account.getAccountType(),
                account.getCurrency(), balance, false);
    }

    private long computeBalance(LedgerAccount account) {
        long debits = ledgerEntryRepository.sumByAccountIdAndDirection(account.getId(), EntryDirection.DR);
        long credits = ledgerEntryRepository.sumByAccountIdAndDirection(account.getId(), EntryDirection.CR);
        return DEBIT_NORMAL_TYPES.contains(account.getAccountType()) ? debits - credits : credits - debits;
    }

    private String cacheKey(String accountCode) {
        return CACHE_KEY_PREFIX + accountCode;
    }
}
