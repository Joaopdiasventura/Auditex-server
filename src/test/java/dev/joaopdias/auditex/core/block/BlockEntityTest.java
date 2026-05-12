package dev.joaopdias.auditex.core.block;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.joaopdias.auditex.core.block.entities.Block;
import dev.joaopdias.auditex.shared.exceptions.ImmutableResourceException;

class BlockEntityTest {

    @Test
    void prePersistSetsCreatedAt() {
        Block block = new Block();

        block.prePersist();

        assertThat(block.getCreatedAt()).isNotNull();
    }

    @Test
    void preUpdateRejectsImmutableBlockChanges() {
        Block block = block();
        block.postLoad();

        block.setHash("b".repeat(64));

        assertThatThrownBy(block::preUpdate)
                .isInstanceOf(ImmutableResourceException.class)
                .hasMessage("Bloco não pode ser alterado");
    }

    private Block block() {
        Block block = new Block();
        block.setId(UUID.randomUUID());
        block.setIndex(0);
        block.setHash("a".repeat(64));
        block.setPreviousHash("0".repeat(64));
        block.setMerkleRoot("c".repeat(64));
        block.setNonce(1L);
        block.setDifficulty(4);
        block.setCreatedAt(Instant.parse("2026-05-11T18:00:00Z"));
        block.setMinedAt(Instant.parse("2026-05-11T18:00:01Z"));
        return block;
    }
}
