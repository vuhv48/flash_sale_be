package io.github.vuhv48.flashsale.auth;

import java.util.UUID;

public interface RegisterWelcomeMailService {

	void enqueueWelcomeEmail(UUID userId, String username, String contactEmail);

	void enqueueAdminNewUserNotification(UUID newUserId, String newUsername, String contactEmail);
}
