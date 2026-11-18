package io.github.vuhv48.flashsale.persistence.repository;

import io.github.vuhv48.flashsale.persistence.entity.EventEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<EventEntity, UUID>, JpaSpecificationExecutor<EventEntity> {

	@Query(value = "SELECT 'EV-' || lpad(nextval('event_code_seq')::text, 6, '0')", nativeQuery = true)
	String nextCode();

	Optional<EventEntity> findByIdAndIsDeletedFalse(UUID id);

	boolean existsByOrganizerIdAndIsDeletedFalseAndStatusNotIn(UUID organizerId, Collection<String> statuses);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from EventEntity e where e.id = :id")
	Optional<EventEntity> lockById(@Param("id") UUID id);

	@Query("""
			select e.id from EventEntity e
			where e.isDeleted = false and (
			  (e.status = 'APPROVED' and e.saleStartsAt <= :now)
			  or (e.status = 'ON_SALE' and e.saleEndsAt <= :now)
			  or (e.status = 'ON_SALE' and not exists (
			      select t.id from TicketTypeEntity t
			      where t.eventId = e.id and t.isDeleted = false
			        and (t.totalQuantity - t.heldQuantity - t.soldQuantity) > 0))
			  or (e.status = 'PAUSED' and e.pausedAt <= :resumeBefore)
			  or (e.status = 'SALE_ENDED' and e.endsAt <= :now)
			)
			""")
	List<UUID> findDueIds(@Param("now") Instant now, @Param("resumeBefore") Instant resumeBefore);
}
