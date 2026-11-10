package io.github.vuhv48.flashsale.persistence.repository;

import io.github.vuhv48.flashsale.persistence.entity.RoleChangeLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleChangeLogRepository extends JpaRepository<RoleChangeLogEntity, Long> {
}
