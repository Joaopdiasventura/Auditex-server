package dev.joaopdias.auditex.core.wallet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.joaopdias.auditex.core.wallet.dto.CreateWalletDto;
import dev.joaopdias.auditex.core.wallet.dto.ReturnWalletDto;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnWalletDto create(@RequestBody @Valid CreateWalletDto createWalletDto) {
        return this.walletService.create(createWalletDto);
    }

}
