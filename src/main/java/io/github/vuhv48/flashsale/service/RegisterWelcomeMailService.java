package io.github.vuhv48.flashsale.service;

import java.util.UUID;

public interface RegisterWelcomeMailService {

	void enqueueWelcomeEmail(UUID userId, String username, String contactEmail);

	void enqueueAdminNewUserNotification(UUID newUserId, String newUsername, String contactEmail);

	void sendPasswordReset(String email, String rawToken);
}
