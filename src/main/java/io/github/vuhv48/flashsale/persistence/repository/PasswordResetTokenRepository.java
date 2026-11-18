package io.github.vuhv48.flashsale.persistence.repository;

import io.github.vuhv48.flashsale.persistence.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

	Optional<PasswordResetTokenEntity> findByTokenHashAndUsedFalse(String tokenHash);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update PasswordResetTokenEntity t set t.used = true where t.userId = :userId and t.used = false")
	void markUnusedAsUsed(@Param("userId") UUID userId);
}
