package io.github.vuhv48.flashsale.persistence.repository;

import io.github.vuhv48.flashsale.persistence.entity.TicketTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketTypeRepository extends JpaRepository<TicketTypeEntity, UUID> {

	List<TicketTypeEntity> findByEventIdAndIsDeletedFalseOrderBySortOrderAscNameAsc(UUID eventId);

	List<TicketTypeEntity> findByEventIdInAndIsDeletedFalse(Collection<UUID> eventIds);

	Optional<TicketTypeEntity> findByIdAndEventIdAndIsDeletedFalse(UUID id, UUID eventId);

	long countByEventIdAndIsDeletedFalse(UUID eventId);

	@Query("""
			select case when count(t) > 0 then true else false end
			from TicketTypeEntity t
			where t.eventId = :eventId and t.isDeleted = false and lower(t.name) = lower(:name)
			  and (:excludeId is null or t.id <> :excludeId)
			""")
	boolean existsName(@Param("eventId") UUID eventId, @Param("name") String name, @Param("excludeId") UUID excludeId);

	@Query("""
			select case when count(t) > 0 then true else false end
			from TicketTypeEntity t
			where t.eventId = :eventId and t.isDeleted = false
			  and (t.totalQuantity - t.heldQuantity - t.soldQuantity) > 0
			""")
	boolean hasStock(@Param("eventId") UUID eventId);

	@Query("""
			select t.id, (t.totalQuantity - t.heldQuantity - t.soldQuantity)
			from TicketTypeEntity t, EventEntity e
			where t.eventId = e.id and t.isDeleted = false and e.isDeleted = false
			  and e.status in ('APPROVED', 'ON_SALE', 'PAUSED')
			  and e.waitingRoomOpensAt <= :seedBefore
			""")
	List<Object[]> stocksToSeed(@Param("seedBefore") Instant seedBefore);
}
