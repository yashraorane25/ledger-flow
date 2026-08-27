package com.ledgerflow.ledger_service.reconciliation;

import com.ledgerflow.ledger_service.reconciliation.dto.ReconciliationReportResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;
    private final ReconciliationReportRepository reconciliationReportRepository;

    public ReconciliationController(ReconciliationService reconciliationService,
                                     ReconciliationReportRepository reconciliationReportRepository) {
        this.reconciliationService = reconciliationService;
        this.reconciliationReportRepository = reconciliationReportRepository;
    }

    // Manual trigger, mainly so this is demoable on-demand instead of only
    // via the 60s scheduled run.
    @PostMapping("/run")
    public List<ReconciliationReportResponse> runNow() {
        return reconciliationService.run().stream()
                .map(ReconciliationReportResponse::from)
                .toList();
    }

    @GetMapping("/reports")
    public List<ReconciliationReportResponse> reports() {
        return reconciliationReportRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(ReconciliationReportResponse::from)
                .toList();
    }
}
