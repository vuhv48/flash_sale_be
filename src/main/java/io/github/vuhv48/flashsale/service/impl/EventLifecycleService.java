package io.github.vuhv48.flashsale.service.impl;

import io.github.vuhv48.flashsale.event.EventStatus;
import io.github.vuhv48.flashsale.persistence.entity.EventEntity;
import io.github.vuhv48.flashsale.persistence.repository.EventRepository;
import io.github.vuhv48.flashsale.persistence.repository.TicketTypeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class EventLifecycleService {

	private final EventRepository eventRepository;
	private final TicketTypeRepository ticketTypeRepository;
	private final EventFlowRecorder flowRecorder;
	private final EventNotifier eventNotifier;
	private final EventCache eventCache;
	private final TransactionTemplate transactionTemplate;

	public EventLifecycleService(
			EventRepository eventRepository,
			TicketTypeRepository ticketTypeRepository,
			EventFlowRecorder flowRecorder,
			EventNotifier eventNotifier,
			EventCache eventCache,
			PlatformTransactionManager transactionManager
	) {
		this.eventRepository = eventRepository;
		this.ticketTypeRepository = ticketTypeRepository;
		this.flowRecorder = flowRecorder;
		this.eventNotifier = eventNotifier;
		this.eventCache = eventCache;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Scheduled(fixedRate = 1000)
	public void tick() {
		Instant now = Instant.now();
		for (UUID id : eventRepository.findDueIds(now, now.minus(Duration.ofMinutes(30)))) {
			try {
				transactionTemplate.executeWithoutResult(status -> advanceOne(id));
			} catch (RuntimeException ex) {
				log.warn("[event] không chuyển được trạng thái {}: {}", id, ex.getMessage());
			}
		}
		Instant seedBefore = now.plus(Duration.ofMinutes(2));
		for (Object[] row : ticketTypeRepository.stocksToSeed(seedBefore)) {
			eventCache.seedInventory((UUID) row[0], ((Number) row[1]).intValue());
		}
	}

	private void advanceOne(UUID eventId) {
		EventEntity event = eventRepository.lockById(eventId).orElse(null);
		if (event == null || event.isDeleted()) {
			return;
		}
		Instant now = Instant.now();
		String from = event.getStatus();
		String next = null;
		String action = null;
		if (EventStatus.APPROVED.equals(from) && event.getSaleStartsAt() != null && !event.getSaleStartsAt().isAfter(now)) {
			boolean ended = (event.getSaleEndsAt() != null && !event.getSaleEndsAt().isAfter(now)) || !ticketTypeRepository.hasStock(event.getId());
			next = ended ? EventStatus.SALE_ENDED : EventStatus.ON_SALE;
			action = ended ? "END_SALE" : "OPEN_SALE";
		} else if (EventStatus.ON_SALE.equals(from) && event.getSaleEndsAt() != null && !event.getSaleEndsAt().isAfter(now)) {
			next = EventStatus.SALE_ENDED;
			action = "END_SALE";
		} else if (EventStatus.ON_SALE.equals(from) && !ticketTypeRepository.hasStock(event.getId())) {
			next = EventStatus.SALE_ENDED;
			action = "SOLD_OUT";
		} else if (EventStatus.PAUSED.equals(from) && event.getPausedAt() != null
				&& !event.getPausedAt().plus(Duration.ofMinutes(30)).isAfter(now)) {
			boolean ended = (event.getSaleEndsAt() != null && !event.getSaleEndsAt().isAfter(now)) || !ticketTypeRepository.hasStock(event.getId());
			next = ended ? EventStatus.SALE_ENDED : EventStatus.ON_SALE;
			action = ended ? "END_SALE" : "RESUME";
			event.setPausedAt(null);
		} else if (EventStatus.SALE_ENDED.equals(from) && event.getEndsAt() != null && !event.getEndsAt().isAfter(now)) {
			next = EventStatus.COMPLETED;
			action = "COMPLETE";
		}
		if (next == null) {
			return;
		}
		flowRecorder.move(event, next, action, null, "system", null);
		eventNotifier.statusChanged(event, from, next);
		eventCache.evict(event.getId());
	}
}
