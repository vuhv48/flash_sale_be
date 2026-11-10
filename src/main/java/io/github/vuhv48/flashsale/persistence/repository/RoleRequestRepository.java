package io.github.vuhv48.flashsale.persistence.repository;

import io.github.vuhv48.flashsale.persistence.entity.RoleRequestEntity;
import io.github.vuhv48.flashsale.persistence.entity.RoleRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRequestRepository extends JpaRepository<RoleRequestEntity, Long> {

	boolean existsByUserIdAndRoleCodeAndStatusAndIsDeletedFalse(UUID userId, String roleCode, RoleRequestStatus status);

	List<RoleRequestEntity> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

	List<RoleRequestEntity> findByStatusAndIsDeletedFalseOrderByCreatedAtAsc(RoleRequestStatus status);

	Optional<RoleRequestEntity> findByIdAndIsDeletedFalse(Long id);
}
