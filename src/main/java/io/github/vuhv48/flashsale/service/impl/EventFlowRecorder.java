package io.github.vuhv48.flashsale.service.impl;

import io.github.vuhv48.flashsale.event.EventStatus;
import io.github.vuhv48.flashsale.persistence.entity.EventEntity;
import io.github.vuhv48.flashsale.persistence.entity.EventFlowEntity;
import io.github.vuhv48.flashsale.persistence.repository.EventFlowRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class EventFlowRecorder {

	private final EventFlowRepository eventFlowRepository;
	private final FlowCatalog flowCatalog;

	public EventFlowRecorder(EventFlowRepository eventFlowRepository, FlowCatalog flowCatalog) {
		this.eventFlowRepository = eventFlowRepository;
		this.flowCatalog = flowCatalog;
	}

	public void move(EventEntity event, String nextStatus, String action, UUID actorId, String actorName, String reason) {
		UUID fromStepId = event.getCurrentStepId();
		UUID toStepId = flowCatalog.stepId(FlowCatalog.EVENT_LIFECYCLE, nextStatus);
		event.setStatus(nextStatus);
		event.setCurrentStepId(toStepId);
		event.setUpdatedBy(actorName);
		if (EventStatus.DRAFT.equals(nextStatus)) {
			event.setRejectReason(null);
		}
		record(event.getId(), fromStepId, toStepId, action, actorId, actorName, reason);
	}

	public void recordFirst(EventEntity event, UUID actorId, String actorName) {
		UUID stepId = flowCatalog.stepId(FlowCatalog.EVENT_LIFECYCLE, event.getStatus());
		record(event.getId(), null, stepId, "CREATE", actorId, actorName, null);
	}

	private void record(UUID eventId, UUID fromStepId, UUID toStepId, String action, UUID actorId, String actorName, String reason) {
		int next = eventFlowRepository.maxStepNo(eventId) + 1;
		EventFlowEntity row = new EventFlowEntity();
		row.setEventId(eventId);
		row.setStepNo(next);
		row.setFromStepId(next == 1 ? null : fromStepId);
		row.setFlowStepId(toStepId);
		row.setAction(action);
		row.setProcessedBy(actorId);
		row.setProcessedByName(actorName);
		row.setReason(trimReason(reason));
		row.setProcessedAt(Instant.now());
		eventFlowRepository.save(row);
	}

	private static String trimReason(String reason) {
		if (reason == null || reason.isBlank()) {
			return null;
		}
		String trimmed = reason.trim();
		return trimmed.length() <= 500 ? trimmed : trimmed.substring(0, 500);
	}
}
