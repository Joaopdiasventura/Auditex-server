package dev.joaopdias.auditex.core.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.joaopdias.auditex.core.transaction.dto.CreateTransactionDto;
import dev.joaopdias.auditex.core.transaction.dto.TransactionResponseDto;
import dev.joaopdias.auditex.shared.dto.PageResponseDto;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/transaction")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping
    public PageResponseDto<TransactionResponseDto> page(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return transactionService.pageTransactions(page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponseDto create(@RequestBody @Valid CreateTransactionDto request) {
        return transactionService.create(request);
    }

    @GetMapping("/{hash}")
    public TransactionResponseDto findByHash(@PathVariable String hash) {
        return transactionService.findByHash(hash);
    }

    @GetMapping("/hash/{hash}")
    public TransactionResponseDto findByHashExplicit(@PathVariable String hash) {
        return transactionService.findByHash(hash);
    }

    @GetMapping("/public-key")
    public PageResponseDto<TransactionResponseDto> pageByPublicKey(
            @RequestParam String publicKey,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return transactionService.pageByPublicKey(publicKey, page, size);
    }

    @GetMapping("/type/{type}")
    public PageResponseDto<TransactionResponseDto> pageByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return transactionService.pageByType(type, page, size);
    }

    @GetMapping("/processing/{processingId}")
    public PageResponseDto<TransactionResponseDto> pageByProcessingId(
            @PathVariable String processingId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return transactionService.pageByProcessingId(processingId, page, size);
    }

    @GetMapping("/file/{fileHash}")
    public PageResponseDto<TransactionResponseDto> pageByFileHash(
            @PathVariable String fileHash,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return transactionService.pageByFileHash(fileHash, page, size);
    }

}
