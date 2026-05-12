package dev.joaopdias.auditex.core.block;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.joaopdias.auditex.core.block.dto.BlockResponseDto;
import dev.joaopdias.auditex.core.block.dto.BlockTransactionsResponseDto;
import dev.joaopdias.auditex.core.block.dto.ValidateResponseDto;
import dev.joaopdias.auditex.shared.dto.PageResponseDto;

@RestController
@RequestMapping("/block")
public class BlockController {

    @Autowired
    private BlockService blockService;

    @GetMapping
    public PageResponseDto<BlockResponseDto> page(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return blockService.pageBlocks(page, size);
    }

    @GetMapping("/validate")
    public ValidateResponseDto validate(){
        return blockService.validate();
    }

    @GetMapping("/latest")
    public BlockResponseDto findLatestBlock() {
        return blockService.findLastBlock();
    }

    @GetMapping("/last")
    public BlockResponseDto findLastBlock() {
        return blockService.findLastBlock();
    }

    @GetMapping("/id/{id}")
    public BlockResponseDto findById(@PathVariable UUID id) {
        return blockService.findById(id);
    }

    @GetMapping("/hash/{hash}")
    public BlockResponseDto findByHashExplicit(@PathVariable String hash) {
        return blockService.findByHash(hash);
    }

    @GetMapping("/index/{index}")
    public BlockResponseDto findByIndex(@PathVariable Integer index) {
        return blockService.findByIndex(index);
    }

    @GetMapping("/{id}/transaction")
    public BlockTransactionsResponseDto pageTransactions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size) {
        return blockService.pageTransactions(id, page, size);
    }

    @GetMapping("/{hash}")
    public BlockResponseDto findByHash(@PathVariable String hash) {
        return blockService.findByHash(hash);
    }
}
