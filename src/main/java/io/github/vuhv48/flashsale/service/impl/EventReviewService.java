package io.github.vuhv48.flashsale.service.impl;

import io.github.vuhv48.flashsale.common.api.ErrorStatus;
import io.github.vuhv48.flashsale.common.exception.DomainException;
import io.github.vuhv48.flashsale.event.EventReasonRequest;
import io.github.vuhv48.flashsale.event.EventStatus;
import io.github.vuhv48.flashsale.event.OrganizerEventDetail;
import io.github.vuhv48.flashsale.persistence.entity.EventEntity;
import io.github.vuhv48.flashsale.persistence.repository.EventRepository;
import io.github.vuhv48.flashsale.persistence.repository.TicketTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EventReviewService {

	private final EventRepository eventRepository;
	private final TicketTypeRepository ticketTypeRepository;
	private final FlowCatalog flowCatalog;
	private final EventFlowRecorder flowRecorder;
	private final EventCache eventCache;
	private final EventQueryService eventQueryService;
	private final EventNotifier eventNotifier;
	private final EventCancellationWriter cancellationWriter;

	public EventReviewService(
			EventRepository eventRepository,
			TicketTypeRepository ticketTypeRepository,
			FlowCatalog flowCatalog,
			EventFlowRecorder flowRecorder,
			EventCache eventCache,
			EventQueryService eventQueryService,
			EventNotifier eventNotifier,
			EventCancellationWriter cancellationWriter
	) {
		this.eventRepository = eventRepository;
		this.ticketTypeRepository = ticketTypeRepository;
		this.flowCatalog = flowCatalog;
		this.flowRecorder = flowRecorder;
		this.eventCache = eventCache;
		this.eventQueryService = eventQueryService;
		this.eventNotifier = eventNotifier;
		this.cancellationWriter = cancellationWriter;
	}

	@Transactional
	public OrganizerEventDetail approve(UUID actorId, String actorName, UUID eventId) {
		EventEntity event = require(eventId);
		if (!EventStatus.PENDING_REVIEW.equals(event.getStatus())) {
			throw new DomainException(ErrorStatus.EVENT_STATUS_INVALID, "Chỉ duyệt được sự kiện đang chờ duyệt.");
		}
		Instant now = Instant.now();
		event.setReviewedBy(actorId);
		event.setReviewedAt(now);
		event.setRejectReason(null);
		flowRecorder.move(event, EventStatus.APPROVED, "APPROVE", actorId, actorName, null);
		eventNotifier.reviewed(event, true, "", actorId, actorName);
		eventCache.evict(eventId);
		return eventQueryService.organizerDetail(event);
	}

	@Transactional
	public OrganizerEventDetail reject(UUID actorId, String actorName, UUID eventId, EventReasonRequest request) {
		EventEntity event = require(eventId);
		if (!EventStatus.PENDING_REVIEW.equals(event.getStatus())) {
			throw new DomainException(ErrorStatus.EVENT_STATUS_INVALID, "Chỉ từ chối được sự kiện đang chờ duyệt.");
		}
		event.setReviewedBy(actorId);
		event.setReviewedAt(Instant.now());
		event.setRejectReason(request.reason());
		flowRecorder.move(event, EventStatus.REJECTED, "REJECT", actorId, actorName, request.reason());
		eventNotifier.reviewed(event, false, request.reason(), actorId, actorName);
		eventCache.evict(eventId);
		return eventQueryService.organizerDetail(event);
	}

	@Transactional
	public OrganizerEventDetail pause(UUID actorId, String actorName, UUID eventId, boolean admin) {
		EventEntity event = admin ? require(eventId) : owned(eventId, actorId);
		if (!EventStatus.ON_SALE.equals(event.getStatus())) {
			throw new DomainException(ErrorStatus.EVENT_STATUS_INVALID, "Chỉ tạm dừng được sự kiện đang bán.");
		}
		if (!admin && event.getPauseCount() >= 3) {
			throw new DomainException(ErrorStatus.PAUSE_LIMIT, ErrorStatus.PAUSE_LIMIT.defaultMessage());
		}
		event.setPausedAt(Instant.now());
		if (!admin) {
			event.setPauseCount(event.getPauseCount() + 1);
		}
		flowRecorder.move(event, EventStatus.PAUSED, "PAUSE", actorId, actorName, null);
		eventNotifier.statusChanged(event, EventStatus.ON_SALE, EventStatus.PAUSED);
		eventCache.evict(eventId);
		return eventQueryService.organizerDetail(event);
	}

	@Transactional
	public OrganizerEventDetail resume(UUID actorId, String actorName, UUID eventId, boolean admin) {
		EventEntity event = admin ? require(eventId) : owned(eventId, actorId);
		if (!EventStatus.PAUSED.equals(event.getStatus())) {
			throw new DomainException(ErrorStatus.EVENT_STATUS_INVALID, "Sự kiện không ở trạng thái tạm dừng.");
		}
		Instant now = Instant.now();
		boolean ended = event.getSaleEndsAt() != null && !event.getSaleEndsAt().isAfter(now)
				|| !ticketTypeRepository.hasStock(event.getId());
		String next = ended ? EventStatus.SALE_ENDED : EventStatus.ON_SALE;
		event.setPausedAt(null);
		flowRecorder.move(event, next, ended ? "END_SALE" : "RESUME", actorId, actorName, null);
		eventNotifier.statusChanged(event, EventStatus.PAUSED, next);
		eventCache.evict(eventId);
		return eventQueryService.organizerDetail(event);
	}

	@Transactional
	public OrganizerEventDetail cancel(UUID actorId, String actorName, UUID eventId, EventReasonRequest request) {
		EventEntity event = require(eventId);
		if (EventStatus.COMPLETED.equals(event.getStatus())) {
			throw new DomainException(ErrorStatus.EVENT_STATUS_INVALID, "Sự kiện đã diễn ra, không hủy được.");
		}
		if (EventStatus.CANCELLED.equals(event.getStatus())) {
			throw new DomainException(ErrorStatus.EVENT_STATUS_INVALID, "Sự kiện đã hủy.");
		}
		String from = event.getStatus();
		event.setCancelReason(request.reason());
		event.setCancelledBy(actorId);
		event.setCancelledAt(Instant.now());
		flowRecorder.move(event, EventStatus.CANCELLED, "CANCEL", actorId, actorName, request.reason());

		cancellationWriter.releaseHolds(eventId, actorName);
		List<EventCancellationWriter.OrderRow> orders = cancellationWriter.activeOrders(eventId);
		UUID cancelledStep = flowCatalog.stepId(FlowCatalog.ORDER_LIFECYCLE, "CANCELLED");
		UUID refundingStep = flowCatalog.stepId(FlowCatalog.ORDER_LIFECYCLE, "REFUNDING");
		UUID refundFlow = flowCatalog.flowId(FlowCatalog.REFUND_PROCESS);
		UUID refundRequested = flowCatalog.stepId(FlowCatalog.REFUND_PROCESS, "REQUESTED");
		int refunds = 0;
		for (EventCancellationWriter.OrderRow order : orders) {
			boolean paid = "PAID".equals(order.status()) || "ISSUED".equals(order.status());
			if (paid && order.totalAmount() > 0) {
				cancellationWriter.moveOrder(order, "REFUNDING", refundingStep, "REQUEST_REFUND", actorId, actorName, request.reason());
				cancellationWriter.insertRefund(order, refundFlow, refundRequested, actorId, actorName, request.reason());
				refunds++;
			} else {
				cancellationWriter.moveOrder(order, "CANCELLED", cancelledStep, "CANCEL_ORDER", actorId, actorName, request.reason());
			}
			eventNotifier.queueCancellationMail(order.userId(), order.email(), event.getName(), order.orderCode(), order.id(), actorName);
		}
		cancellationWriter.revokeTickets(eventId, actorName, request.reason());
		eventNotifier.cancelled(event, refunds, actorId, actorName);
		eventNotifier.statusChanged(event, from, EventStatus.CANCELLED);
		eventCache.evict(eventId);
		return eventQueryService.organizerDetail(event);
	}

	private EventEntity require(UUID eventId) {
		return eventRepository.findByIdAndIsDeletedFalse(eventId)
				.orElseThrow(() -> new DomainException(ErrorStatus.EVENT_NOT_FOUND, ErrorStatus.EVENT_NOT_FOUND.defaultMessage()));
	}

	private EventEntity owned(UUID eventId, UUID organizerId) {
		EventEntity event = require(eventId);
		if (!event.getOrganizerId().equals(organizerId)) {
			throw new DomainException(ErrorStatus.EVENT_NOT_FOUND, ErrorStatus.EVENT_NOT_FOUND.defaultMessage());
		}
		return event;
	}
}
