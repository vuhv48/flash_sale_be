package io.github.vuhv48.flashsale.persistence.repository;

import io.github.vuhv48.flashsale.persistence.entity.FlowEntity;
import io.github.vuhv48.flashsale.persistence.entity.FlowStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface FlowStepRepository extends JpaRepository<FlowStepEntity, UUID> {

	@Query("""
			select f.code, f.id, s.code, s.id
			from FlowStepEntity s, FlowEntity f
			where s.flowId = f.id and s.isDeleted = false and f.isDeleted = false and f.enabled = true
			""")
	List<Object[]> allEnabledSteps();
}
