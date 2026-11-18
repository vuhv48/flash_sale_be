package io.github.vuhv48.flashsale.persistence.repository;

import io.github.vuhv48.flashsale.persistence.entity.AccountLockLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountLockLogRepository extends JpaRepository<AccountLockLogEntity, Long> {
}
