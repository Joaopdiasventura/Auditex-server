package dev.joaopdias.auditex.core.block;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.joaopdias.auditex.core.block.dto.BlockResponseDto;
import dev.joaopdias.auditex.core.block.dto.ValidateResponseDto;

@RestController
@RequestMapping("/block")
public class BlockController {

    @Autowired
    private BlockService blockService;

    @GetMapping("/validate")
    public ValidateResponseDto validate(){
        return blockService.validate();
    }

    @GetMapping("/last")
    public BlockResponseDto findLastBlock() {
        return blockService.findLastBlock();
    }

    @GetMapping("/{hash}")
    public BlockResponseDto findByHash(@PathVariable String hash) {
        return blockService.findByHash(hash);
    }

}