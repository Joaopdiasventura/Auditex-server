package dev.joaopdias.auditex.core.block;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import dev.joaopdias.auditex.core.block.entities.Block;

@Repository
public interface BlockRepository extends JpaRepository<Block, UUID>{
    Optional<Block> findByHash(String hash);

    Optional<Block> findByIndex(Integer index);

    Optional<Block> findTopByOrderByIndexDesc();

    boolean existsByHash(String hash);

    boolean existsByIndex(Integer index);

    @Query("select b from Block b order by b.index asc")
    Page<Block> pageBlocks(Pageable pageable);

    List<Block> findByIndexGreaterThanOrderByIndexAsc(Integer index, Pageable pageable);
}
