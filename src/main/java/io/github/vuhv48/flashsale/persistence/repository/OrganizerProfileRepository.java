package io.github.vuhv48.flashsale.persistence.repository;

import io.github.vuhv48.flashsale.persistence.entity.OrganizerProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizerProfileRepository extends JpaRepository<OrganizerProfileEntity, UUID> {

	Optional<OrganizerProfileEntity> findByUserIdAndIsDeletedFalse(UUID userId);

	boolean existsByTaxCodeAndIsDeletedFalseAndUserIdNot(String taxCode, UUID userId);
}
