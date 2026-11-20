package io.github.vuhv48.flashsale.persistence.repository;

import io.github.vuhv48.flashsale.persistence.entity.IntegrationOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IntegrationOutboxRepository extends JpaRepository<IntegrationOutboxEntity, UUID> {
}
