package com.ledgerflow.ledger_service.ledger;

import com.ledgerflow.ledger_service.ledger.dto.AccountBalanceResponse;
import com.ledgerflow.ledger_service.ledger.dto.AccountResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final LedgerAccountRepository ledgerAccountRepository;
    private final BalanceService balanceService;

    public AccountController(LedgerAccountRepository ledgerAccountRepository, BalanceService balanceService) {
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.balanceService = balanceService;
    }

    @GetMapping
    public List<AccountResponse> list() {
        return ledgerAccountRepository.findAll().stream()
                .map(AccountResponse::from)
                .toList();
    }

    @GetMapping("/{accountCode}/balance")
    public ResponseEntity<AccountBalanceResponse> balance(@PathVariable String accountCode) {
        return balanceService.getBalance(accountCode)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
