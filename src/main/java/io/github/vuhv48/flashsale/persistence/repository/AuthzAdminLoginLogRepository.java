package io.github.vuhv48.flashsale.persistence.repository;

import io.github.vuhv48.flashsale.persistence.entity.AuthzAdminLoginLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthzAdminLoginLogRepository extends JpaRepository<AuthzAdminLoginLogEntity, Long> {
}
