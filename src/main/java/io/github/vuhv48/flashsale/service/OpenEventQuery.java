package io.github.vuhv48.flashsale.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

public interface OpenEventQuery {

	boolean hasUnfinishedEvent(UUID organizerUserId);
}

@Component
class NoOpenEventQuery implements OpenEventQuery {

	@Override
	public boolean hasUnfinishedEvent(UUID organizerUserId) {
		return false;
	}
}
