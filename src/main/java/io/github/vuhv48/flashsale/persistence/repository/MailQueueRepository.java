package io.github.vuhv48.flashsale.persistence.repository;

import io.github.vuhv48.flashsale.persistence.entity.MailQueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MailQueueRepository extends JpaRepository<MailQueueEntity, UUID> {
}
