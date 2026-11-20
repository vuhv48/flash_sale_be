package io.github.vuhv48.flashsale.persistence.repository;

import io.github.vuhv48.flashsale.persistence.entity.SystemConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SystemConfigRepository extends JpaRepository<SystemConfigEntity, UUID> {

	List<SystemConfigEntity> findByGroupCodeAndActiveTrueAndIsDeletedFalseOrderByOrderNoAsc(String groupCode);

	Optional<SystemConfigEntity> findByGroupCodeAndCodeAndActiveTrueAndIsDeletedFalse(String groupCode, String code);
}
