package io.github.vuhv48.flashsale.persistence.repository;

import io.github.vuhv48.flashsale.persistence.entity.FlowEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FlowEntityRepository extends JpaRepository<FlowEntity, UUID> {
}
