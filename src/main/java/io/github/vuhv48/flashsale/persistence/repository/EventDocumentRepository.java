package io.github.vuhv48.flashsale.persistence.repository;

import io.github.vuhv48.flashsale.persistence.entity.EventDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventDocumentRepository extends JpaRepository<EventDocumentEntity, UUID> {

	List<EventDocumentEntity> findByEventIdAndIsDeletedFalseOrderBySortOrderAsc(UUID eventId);

	List<EventDocumentEntity> findByEventIdInAndIsDeletedFalse(Collection<UUID> eventIds);

	Optional<EventDocumentEntity> findByIdAndEventIdAndIsDeletedFalse(UUID id, UUID eventId);

	long countByEventIdAndDocumentTypeCodeAndIsDeletedFalse(UUID eventId, String documentTypeCode);

	@Query("""
			select d from EventDocumentEntity d
			where d.eventId = :eventId and d.documentTypeCode = :type and d.isDeleted = false
			""")
	List<EventDocumentEntity> findActive(@Param("eventId") UUID eventId, @Param("type") String type);
}
