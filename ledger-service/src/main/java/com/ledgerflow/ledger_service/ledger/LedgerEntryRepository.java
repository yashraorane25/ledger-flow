package com.ledgerflow.ledger_service.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    @Query("select coalesce(sum(e.amountMinor), 0) from LedgerEntry e "
            + "where e.accountId = :accountId and e.direction = :direction")
    long sumByAccountIdAndDirection(@Param("accountId") UUID accountId, @Param("direction") EntryDirection direction);
}
