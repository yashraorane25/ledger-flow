package com.ledgerflow.ledger_service.ledger;

import com.ledgerflow.ledger_service.ledger.dto.AccountBalanceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private LedgerAccountRepository ledgerAccountRepository;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private BalanceService balanceService;

    @BeforeEach
    void setUp() {
        balanceService = new BalanceService(ledgerAccountRepository, ledgerEntryRepository, redisTemplate);
    }

    @Test
    void cacheMissComputesDebitNormalBalanceAndCachesIt() {
        LedgerAccount pspReceivable = new LedgerAccount(UUID.randomUUID(), "PSP_RECEIVABLE", "ASSET", "USD");
        when(ledgerAccountRepository.findByAccountCode("PSP_RECEIVABLE")).thenReturn(Optional.of(pspReceivable));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ledger:balance:PSP_RECEIVABLE")).thenReturn(null);
        when(ledgerEntryRepository.sumByAccountIdAndDirection(pspReceivable.getId(), EntryDirection.DR)).thenReturn(500L);
        when(ledgerEntryRepository.sumByAccountIdAndDirection(pspReceivable.getId(), EntryDirection.CR)).thenReturn(200L);

        Optional<AccountBalanceResponse> result = balanceService.getBalance("PSP_RECEIVABLE");

        assertTrue(result.isPresent());
        assertEquals(300L, result.get().balanceMinor());
        assertFalse(result.get().cached());
        verify(valueOperations).set(eq("ledger:balance:PSP_RECEIVABLE"), eq("300"), any(Duration.class));
    }

    @Test
    void creditNormalAccountFlipsTheSign() {
        LedgerAccount merchantPayable = new LedgerAccount(UUID.randomUUID(), "MERCHANT_PAYABLE", "LIABILITY", "USD");
        when(ledgerAccountRepository.findByAccountCode("MERCHANT_PAYABLE")).thenReturn(Optional.of(merchantPayable));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ledger:balance:MERCHANT_PAYABLE")).thenReturn(null);
        when(ledgerEntryRepository.sumByAccountIdAndDirection(merchantPayable.getId(), EntryDirection.DR)).thenReturn(100L);
        when(ledgerEntryRepository.sumByAccountIdAndDirection(merchantPayable.getId(), EntryDirection.CR)).thenReturn(400L);

        Optional<AccountBalanceResponse> result = balanceService.getBalance("MERCHANT_PAYABLE");

        assertEquals(300L, result.get().balanceMinor());
    }

    @Test
    void cacheHitSkipsDatabaseLookup() {
        LedgerAccount pspReceivable = new LedgerAccount(UUID.randomUUID(), "PSP_RECEIVABLE", "ASSET", "USD");
        when(ledgerAccountRepository.findByAccountCode("PSP_RECEIVABLE")).thenReturn(Optional.of(pspReceivable));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ledger:balance:PSP_RECEIVABLE")).thenReturn("777");

        Optional<AccountBalanceResponse> result = balanceService.getBalance("PSP_RECEIVABLE");

        assertEquals(777L, result.get().balanceMinor());
        assertTrue(result.get().cached());
        verify(ledgerEntryRepository, never()).sumByAccountIdAndDirection(any(), any());
    }

    @Test
    void unknownAccountCodeReturnsEmpty() {
        when(ledgerAccountRepository.findByAccountCode("NOPE")).thenReturn(Optional.empty());

        Optional<AccountBalanceResponse> result = balanceService.getBalance("NOPE");

        assertFalse(result.isPresent());
    }

    @Test
    void evictDeletesTheCacheKey() {
        balanceService.evict("PSP_RECEIVABLE");

        verify(redisTemplate).delete("ledger:balance:PSP_RECEIVABLE");
    }
}
