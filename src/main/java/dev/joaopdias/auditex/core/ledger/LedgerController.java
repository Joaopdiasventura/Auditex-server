package dev.joaopdias.auditex.core.ledger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.joaopdias.auditex.core.block.dto.ValidateResponseDto;
import dev.joaopdias.auditex.core.ledger.dto.LedgerStatusResponseDto;

@RestController
@RequestMapping("/ledger")
public class LedgerController {

    @Autowired
    private LedgerService ledgerService;

    @GetMapping("/status")
    public LedgerStatusResponseDto status() {
        return ledgerService.status();
    }

    @GetMapping("/validate")
    public ValidateResponseDto validate() {
        return ledgerService.validate();
    }
}
