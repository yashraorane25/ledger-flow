package com.ledgerflow.ledger_service.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByJournalId(UUID journalId);

    @Query("select coalesce(sum(e.amountMinor), 0) from LedgerEntry e "
            + "where e.accountId = :accountId and e.direction = :direction")
    long sumByAccountIdAndDirection(@Param("accountId") UUID accountId, @Param("direction") EntryDirection direction);

    @Query("select coalesce(sum(e.amountMinor), 0) from LedgerEntry e "
            + "where e.journalId = :journalId and e.direction = :direction")
    long sumByJournalIdAndDirection(@Param("journalId") UUID journalId, @Param("direction") EntryDirection direction);
}
