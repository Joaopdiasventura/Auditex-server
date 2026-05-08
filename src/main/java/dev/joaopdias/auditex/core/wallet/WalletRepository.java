package dev.joaopdias.auditex.core.wallet;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.joaopdias.auditex.core.wallet.entities.Wallet;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

}
