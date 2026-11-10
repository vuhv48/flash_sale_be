package io.github.vuhv48.flashsale.auth;

import io.github.vuhv48.flashsale.security.user.AppUserDetails;

import java.util.List;
import java.util.UUID;

public interface AuthAccountService {

	RegisterOutcome register(
			String username,
			String rawPassword,
			String contactEmail,
			String fullName,
			String phone);

	AccessRefreshResult issueTokens(AppUserDetails principal);

	AccessRefreshResult rotateWithRefreshToken(String rawRefreshToken);

	void changePassword(UUID userId, String currentPassword, String newPassword);

	void logout(UUID userId);

	void setUserEnabledByUsername(String username, boolean enabled);

	List<AdminUserSearchResponse> searchUsersByName(String name);

	void recordAdminLoginSuccess(UUID userId, String username, String ipAddress, String userAgent);
}
