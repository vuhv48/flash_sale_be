package io.github.vuhv48.flashsale.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.vuhv48.flashsale.persistence.entity.AuditLogEntity;
import io.github.vuhv48.flashsale.persistence.entity.EventEntity;
import io.github.vuhv48.flashsale.persistence.entity.IntegrationOutboxEntity;
import io.github.vuhv48.flashsale.persistence.entity.MailQueueEntity;
import io.github.vuhv48.flashsale.persistence.repository.AuditLogRepository;
import io.github.vuhv48.flashsale.persistence.repository.IntegrationOutboxRepository;
import io.github.vuhv48.flashsale.persistence.repository.MailQueueRepository;
import io.github.vuhv48.flashsale.persistence.repository.UserAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class EventNotifier {

	private final IntegrationOutboxRepository outboxRepository;
	private final MailQueueRepository mailQueueRepository;
	private final AuditLogRepository auditLogRepository;
	private final UserAccountRepository userAccountRepository;
	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	public EventNotifier(
			IntegrationOutboxRepository outboxRepository,
			MailQueueRepository mailQueueRepository,
			AuditLogRepository auditLogRepository,
			UserAccountRepository userAccountRepository
	) {
		this.outboxRepository = outboxRepository;
		this.mailQueueRepository = mailQueueRepository;
		this.auditLogRepository = auditLogRepository;
		this.userAccountRepository = userAccountRepository;
	}

	public void submitted(EventEntity event, String actorName) {
		log.info("[event] {} gửi duyệt sự kiện {} {}", actorName, event.getCode(), event.getName());
		outbox(event, "event.submitted", Map.of("code", event.getCode(), "name", event.getName(), "organizerId", event.getOrganizerId()));
		audit(event.getOrganizerId(), actorName, "EVENT_SUBMITTED", event, null);
	}

	public void reviewed(EventEntity event, boolean approved, String reason, UUID actorId, String actorName) {
		String type = approved ? "event.approved" : "event.rejected";
		outbox(event, type, Map.of("code", event.getCode(), "name", event.getName(), "reason", reason == null ? "" : reason));
		audit(actorId, actorName, approved ? "EVENT_APPROVED" : "EVENT_REJECTED", event, reason);
		userAccountRepository.findActiveById(event.getOrganizerId()).ifPresent(user -> {
			if (user.getEmail() == null || !user.getEmail().contains("@")) {
				log.info("[mail] sự kiện {} không có email ban tổ chức", event.getCode());
				return;
			}
			String result = approved ? "đã được duyệt" : "bị từ chối";
			enqueue(user.getId(), user.getEmail(), "EVENT_REVIEWED",
					Map.of("eventName", event.getName(), "result", result, "reason", reason == null ? "" : reason),
					"EVENT", event.getId().toString(),
					"EVENT_REVIEWED:" + event.getId() + ":" + event.getReviewedAt().toEpochMilli(),
					actorName);
		});
	}

	public void statusChanged(EventEntity event, String from, String to) {
		outbox(event, "event.status-changed", Map.of("code", event.getCode(), "from", from, "to", to));
	}

	public void cancelled(EventEntity event, int refundCount, UUID actorId, String actorName) {
		outbox(event, "event.cancelled", Map.of("code", event.getCode(), "name", event.getName(), "refundCount", refundCount));
		audit(actorId, actorName, "EVENT_CANCELLED", event, event.getCancelReason());
	}

	public void queueCancellationMail(UUID userId, String email, String eventName, String orderCode, UUID orderId, String actorName) {
		if (email == null || !email.contains("@")) {
			return;
		}
		enqueue(userId, email, "EVENT_CANCELLED",
				Map.of("eventName", eventName, "orderCode", orderCode),
				"ORDER", orderId.toString(),
				"EVENT_CANCELLED:" + orderId,
				actorName);
	}

	private void enqueue(UUID userId, String email, String template, Map<String, ?> variables, String refType, String refId, String idempotencyKey, String actorName) {
		MailQueueEntity mail = new MailQueueEntity();
		mail.setUserId(userId);
		mail.setToAddress(email);
		mail.setTemplateCode(template);
		mail.setVariables(json(variables));
		mail.setRefType(refType);
		mail.setRefId(refId);
		mail.setIdempotencyKey(idempotencyKey);
		mail.setCreatedBy(actorName);
		mail.setUpdatedBy(actorName);
		mailQueueRepository.save(mail);
	}

	private void outbox(EventEntity event, String type, Map<String, ?> payload) {
		IntegrationOutboxEntity row = new IntegrationOutboxEntity();
		row.setAggregateType("EVENT");
		row.setAggregateId(event.getId());
		row.setTopic(type);
		row.setMessageKey(event.getId().toString());
		row.setEventType(type);
		row.setPayload(json(Map.of("eventId", event.getId().toString(), "data", payload)));
		row.setCreatedAt(Instant.now());
		outboxRepository.save(row);
	}

	private void audit(UUID actorId, String actorName, String action, EventEntity event, String reason) {
		AuditLogEntity row = new AuditLogEntity();
		row.setOccurredAt(Instant.now());
		row.setActorId(actorId);
		row.setActorName(actorName);
		row.setAction(action);
		row.setEntityType("EVENT");
		row.setEntityId(event.getId().toString());
		row.setReason(reason);
		auditLogRepository.save(row);
	}

	private String json(Map<String, ?> payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Không tạo được JSON", ex);
		}
	}
}
