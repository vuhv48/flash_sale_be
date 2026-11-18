package io.github.vuhv48.flashsale.service;

import java.util.UUID;

public interface OpenEventQuery {

	boolean hasUnfinishedEvent(UUID organizerUserId);
}
