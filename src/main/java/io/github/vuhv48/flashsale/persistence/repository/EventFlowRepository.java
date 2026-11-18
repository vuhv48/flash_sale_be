package io.github.vuhv48.flashsale.persistence.repository;

import io.github.vuhv48.flashsale.persistence.entity.EventFlowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface EventFlowRepository extends JpaRepository<EventFlowEntity, UUID> {

	@Query("select coalesce(max(f.stepNo), 0) from EventFlowEntity f where f.eventId = :eventId")
	int maxStepNo(@Param("eventId") UUID eventId);
}
