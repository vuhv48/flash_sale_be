package io.github.vuhv48.flashsale.service;

import io.github.vuhv48.flashsale.auth.AccessRefreshResult;
import io.github.vuhv48.flashsale.auth.AdminUserSearchResponse;
import io.github.vuhv48.flashsale.auth.ProfileResponse;
import io.github.vuhv48.flashsale.auth.RegisterOutcome;
import io.github.vuhv48.flashsale.security.user.AppUserDetails;

import java.util.List;
import java.util.UUID;

public interface AuthAccountService {

	RegisterOutcome register(String email, String rawPassword, String fullName, String phone);

	AccessRefreshResult login(String email, String rawPassword, String ipAddress, String userAgent);

	AccessRefreshResult issueTokens(AppUserDetails principal);

	AccessRefreshResult rotateWithRefreshToken(String rawRefreshToken);

	void changePassword(UUID userId, String currentPassword, String newPassword);

	void logout(UUID userId);

	ProfileResponse profile(UUID userId);

	ProfileResponse updateProfile(UUID userId, String fullName, String phone);

	void requestPasswordReset(String email);

	void resetPassword(String rawToken, String newPassword);

	void lockAccount(UUID actorId, String username, String reason);

	void unlockAccount(UUID actorId, String username, String reason);

	List<AdminUserSearchResponse> searchUsers(String name);

	void recordAdminLoginSuccess(UUID userId, String username, String ipAddress, String userAgent);
}
