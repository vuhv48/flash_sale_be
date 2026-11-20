package io.github.vuhv48.flashsale.service.impl;

import io.github.vuhv48.flashsale.event.EventStatus;
import io.github.vuhv48.flashsale.persistence.repository.EventRepository;
import io.github.vuhv48.flashsale.service.OpenEventQuery;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OpenEventQueryImpl implements OpenEventQuery {

	private final EventRepository eventRepository;

	public OpenEventQueryImpl(EventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	@Override
	public boolean hasUnfinishedEvent(UUID organizerUserId) {
		return eventRepository.existsByOrganizerIdAndIsDeletedFalseAndStatusNotIn(organizerUserId, EventStatus.FINISHED);
	}
}
