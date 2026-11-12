package io.github.vuhv48.flashsale.service.impl;

import io.github.vuhv48.flashsale.auth.AccessRefreshResult;
import io.github.vuhv48.flashsale.auth.AdminUserSearchResponse;
import io.github.vuhv48.flashsale.auth.PasswordPolicy;
import io.github.vuhv48.flashsale.auth.ProfileResponse;
import io.github.vuhv48.flashsale.auth.RegisterOutcome;
import io.github.vuhv48.flashsale.common.api.ErrorStatus;
import io.github.vuhv48.flashsale.common.exception.DomainException;
import io.github.vuhv48.flashsale.common.security.LoadUserForSecurityPort;
import io.github.vuhv48.flashsale.persistence.entity.AccountLockLogEntity;
import io.github.vuhv48.flashsale.persistence.entity.AuthzAdminLoginLogEntity;
import io.github.vuhv48.flashsale.persistence.entity.PasswordResetTokenEntity;
import io.github.vuhv48.flashsale.persistence.entity.RefreshTokenEntity;
import io.github.vuhv48.flashsale.persistence.entity.RoleEntity;
import io.github.vuhv48.flashsale.persistence.entity.UserAccount;
import io.github.vuhv48.flashsale.persistence.repository.AccountLockLogRepository;
import io.github.vuhv48.flashsale.persistence.repository.AuthzAdminLoginLogRepository;
import io.github.vuhv48.flashsale.persistence.repository.PasswordResetTokenRepository;
import io.github.vuhv48.flashsale.persistence.repository.RefreshTokenRepository;
import io.github.vuhv48.flashsale.persistence.repository.RoleEntityRepository;
import io.github.vuhv48.flashsale.persistence.repository.UserAccountRepository;
import io.github.vuhv48.flashsale.security.config.JwtProperties;
import io.github.vuhv48.flashsale.security.jwt.JwtService;
import io.github.vuhv48.flashsale.security.user.AppUserDetails;
import io.github.vuhv48.flashsale.service.AuthAccountService;
import io.github.vuhv48.flashsale.service.RegisterWelcomeMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthAccountServiceImpl implements AuthAccountService {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final String DEFAULT_SELF_REGISTER_ROLE = "CUSTOMER";

	private final JwtService jwtService;
	private final JwtProperties jwtProperties;
	private final LoadUserForSecurityPort loadUserForSecurity;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserAccountRepository userAccountRepository;
	private final RoleEntityRepository roleEntityRepository;
	private final PasswordEncoder passwordEncoder;
	private final RegisterWelcomeMailService registerWelcomeMailService;
	private final AuthzAdminLoginLogRepository authzAdminLoginLogRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final AccountLockLogRepository accountLockLogRepository;

	@Override
	@Transactional
	public RegisterOutcome register(String email, String rawPassword, String fullName, String phone) {
		String normalizedEmail = normalizeEmail(email);
		PasswordPolicy.check(rawPassword, normalizedEmail);
		if (userAccountRepository.existsByEmail(normalizedEmail) || userAccountRepository.existsByUsername(normalizedEmail)) {
			throw new DomainException(ErrorStatus.EMAIL_TAKEN, ErrorStatus.EMAIL_TAKEN.defaultMessage());
		}
		if (userAccountRepository.existsByPhone(phone)) {
			throw new DomainException(ErrorStatus.PHONE_TAKEN, ErrorStatus.PHONE_TAKEN.defaultMessage());
		}
		RoleEntity userRole = roleEntityRepository.findByCodeAndIsDeletedFalseAndEnabledTrue(DEFAULT_SELF_REGISTER_ROLE)
				.orElseThrow(() -> new DomainException(
						ErrorStatus.ILLEGAL_STATE,
						"Chưa cấu hình role CUSTOMER trong CSDL. Chạy migration Flyway."));
		UserAccount acc = new UserAccount();
		acc.setUsername(normalizedEmail);
		acc.setEmail(normalizedEmail);
		acc.setFullName(fullName.trim());
		acc.setPhone(phone.trim());
		acc.setPasswordHash(passwordEncoder.encode(rawPassword));
		acc.setEnabled(true);
		acc.setDataScope("OWN");
		acc.getRoles().add(userRole);
		userAccountRepository.save(acc);
		registerWelcomeMailService.enqueueWelcomeEmail(acc.getId(), normalizedEmail, normalizedEmail);
		registerWelcomeMailService.enqueueAdminNewUserNotification(acc.getId(), normalizedEmail, normalizedEmail);
		var snap = loadUserForSecurity.loadByEmail(normalizedEmail)
				.orElseThrow(() -> new DomainException(ErrorStatus.INTERNAL_ERROR, ErrorStatus.INTERNAL_ERROR.defaultMessage()));
		return new RegisterOutcome(issueTokens(new AppUserDetails(snap)), "Đăng ký thành công.");
	}

	@Override
	@Transactional
	public AccessRefreshResult login(String email, String rawPassword, String ipAddress, String userAgent) {
		String normalizedEmail = normalizeEmail(email);
		UserAccount user = userAccountRepository.findByEmail(normalizedEmail).orElse(null);
		if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
			throw new DomainException(ErrorStatus.AUTH_FAILED, ErrorStatus.AUTH_FAILED.defaultMessage());
		}
		if (!user.isEnabled()) {
			throw new DomainException(ErrorStatus.ACCOUNT_DISABLED, ErrorStatus.ACCOUNT_DISABLED.defaultMessage());
		}
		var snap = loadUserForSecurity.loadByUserId(user.getId())
				.orElseThrow(() -> new DomainException(ErrorStatus.USER_NOT_FOUND, ErrorStatus.USER_NOT_FOUND.defaultMessage()));
		recordAdminLoginSuccess(user.getId(), user.getUsername(), ipAddress, userAgent);
		return issueTokens(new AppUserDetails(snap));
	}

	private UserAccount findByLoginName(String username) {
		String value = username == null ? "" : username.trim();
		return userAccountRepository.findByUsername(value)
				.or(() -> userAccountRepository.findByEmail(value.toLowerCase()))
				.orElseThrow(() -> new DomainException(ErrorStatus.USER_NOT_FOUND, ErrorStatus.USER_NOT_FOUND.defaultMessage()));
	}

	private UserAccount activeUser(UUID userId) {
		return userAccountRepository.findActiveById(userId)
				.orElseThrow(() -> new DomainException(ErrorStatus.USER_NOT_FOUND, ErrorStatus.USER_NOT_FOUND.defaultMessage()));
	}

	private static ProfileResponse toProfile(UserAccount user) {
		return new ProfileResponse(user.getEmail(), user.getFullName(), user.getPhone(), user.isEnabled());
	}

	private void writeLockLog(UUID userId, UUID actorId, String action, String reason) {
		AccountLockLogEntity log = new AccountLockLogEntity();
		log.setUserId(userId);
		log.setActorUserId(actorId);
		log.setAction(action);
		log.setReason(reason.trim());
		accountLockLogRepository.save(log);
	}

	private static String normalizeEmail(String contactEmail) {
		if (contactEmail == null || contactEmail.isBlank()) {
			return null;
		}
		return contactEmail.trim().toLowerCase();
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	@Override
	@Transactional
	public AccessRefreshResult issueTokens(AppUserDetails principal) {
		refreshTokenRepository.revokeAllActiveForUser(principal.getId());
		String access = jwtService.generateAccessToken(principal, principal.getId());
		String refresh = createAndPersistRefreshToken(principal.getId());
		return new AccessRefreshResult(
				access,
				refresh,
				jwtProperties.expirationSeconds(),
				jwtProperties.refreshExpirationSeconds());
	}

	@Override
	@Transactional
	public AccessRefreshResult rotateWithRefreshToken(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new DomainException(ErrorStatus.REFRESH_TOKEN_INVALID, ErrorStatus.REFRESH_TOKEN_INVALID.defaultMessage());
		}
		String hash = sha256Hex(rawRefreshToken.trim());
		var rowOpt = refreshTokenRepository.findByTokenHashAndRevokedIsFalse(hash);
		if (rowOpt.isEmpty()) {
			throw new DomainException(ErrorStatus.REFRESH_TOKEN_INVALID, ErrorStatus.REFRESH_TOKEN_INVALID.defaultMessage());
		}
		RefreshTokenEntity row = rowOpt.get();
		if (row.getExpiresAt().isBefore(Instant.now())) {
			row.setRevoked(true);
			refreshTokenRepository.save(row);
			throw new DomainException(ErrorStatus.REFRESH_TOKEN_INVALID, ErrorStatus.REFRESH_TOKEN_INVALID.defaultMessage());
		}
		row.setRevoked(true);
		refreshTokenRepository.save(row);

		var snap = loadUserForSecurity.loadByUserId(row.getUserId())
				.orElseThrow(() -> new DomainException(ErrorStatus.USER_NOT_FOUND, ErrorStatus.USER_NOT_FOUND.defaultMessage()));
		if (!snap.enabled()) {
			throw new DomainException(ErrorStatus.ACCOUNT_DISABLED, ErrorStatus.ACCOUNT_DISABLED.defaultMessage());
		}
		var principal = new AppUserDetails(snap);
		String access = jwtService.generateAccessToken(principal, principal.getId());
		String refresh = createAndPersistRefreshToken(row.getUserId());
		return new AccessRefreshResult(
				access,
				refresh,
				jwtProperties.expirationSeconds(),
				jwtProperties.refreshExpirationSeconds());
	}

	@Override
	@Transactional
	public void changePassword(UUID userId, String currentPassword, String newPassword) {
		UserAccount u = userAccountRepository.findActiveById(userId)
				.orElseThrow(() -> new DomainException(ErrorStatus.USER_NOT_FOUND, ErrorStatus.USER_NOT_FOUND.defaultMessage()));
		if (!passwordEncoder.matches(currentPassword, u.getPasswordHash())) {
			throw new DomainException(ErrorStatus.PASSWORD_MISMATCH, ErrorStatus.PASSWORD_MISMATCH.defaultMessage());
		}
		PasswordPolicy.check(newPassword, u.getEmail());
		u.setPasswordHash(passwordEncoder.encode(newPassword));
		userAccountRepository.save(u);
		refreshTokenRepository.revokeAllActiveForUser(userId);
	}

	@Override
	@Transactional
	public void logout(UUID userId) {
		refreshTokenRepository.revokeAllActiveForUser(userId);
	}

	@Override
	@Transactional
	public ProfileResponse profile(UUID userId) {
		return toProfile(activeUser(userId));
	}

	@Override
	@Transactional
	public ProfileResponse updateProfile(UUID userId, String fullName, String phone) {
		UserAccount user = activeUser(userId);
		if (userAccountRepository.existsByPhoneAndIdNot(phone, userId)) {
			throw new DomainException(ErrorStatus.PHONE_TAKEN, ErrorStatus.PHONE_TAKEN.defaultMessage());
		}
		user.setFullName(fullName.trim());
		user.setPhone(phone.trim());
		userAccountRepository.save(user);
		return toProfile(user);
	}

	@Override
	@Transactional
	public void requestPasswordReset(String email) {
		String normalizedEmail = normalizeEmail(email);
		UserAccount user = userAccountRepository.findByEmail(normalizedEmail).orElse(null);
		if (user == null || !user.isEnabled()) {
			return;
		}
		passwordResetTokenRepository.markUnusedAsUsed(user.getId());
		byte[] rnd = new byte[32];
		RANDOM.nextBytes(rnd);
		String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
		PasswordResetTokenEntity token = new PasswordResetTokenEntity();
		token.setUserId(user.getId());
		token.setTokenHash(sha256Hex(raw));
		token.setExpiresAt(Instant.now().plusSeconds(30 * 60));
		token.setUsed(false);
		passwordResetTokenRepository.save(token);
		registerWelcomeMailService.sendPasswordReset(normalizedEmail, raw);
	}

	@Override
	@Transactional
	public void resetPassword(String rawToken, String newPassword) {
		PasswordResetTokenEntity token = passwordResetTokenRepository.findByTokenHashAndUsedFalse(sha256Hex(rawToken.trim()))
				.orElseThrow(() -> new DomainException(ErrorStatus.RESET_TOKEN_INVALID, ErrorStatus.RESET_TOKEN_INVALID.defaultMessage()));
		if (token.getExpiresAt().isBefore(Instant.now())) {
			throw new DomainException(ErrorStatus.RESET_TOKEN_INVALID, ErrorStatus.RESET_TOKEN_INVALID.defaultMessage());
		}
		UserAccount user = activeUser(token.getUserId());
		PasswordPolicy.check(newPassword, user.getEmail());
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		userAccountRepository.save(user);
		token.setUsed(true);
		passwordResetTokenRepository.save(token);
		refreshTokenRepository.revokeAllActiveForUser(user.getId());
	}

	@Override
	@Transactional
	public void lockAccount(UUID actorId, String username, String reason) {
		UserAccount user = findByLoginName(username);
		if (actorId.equals(user.getId())) {
			throw new DomainException(ErrorStatus.CANNOT_LOCK_SELF, ErrorStatus.CANNOT_LOCK_SELF.defaultMessage());
		}
		user.setEnabled(false);
		userAccountRepository.save(user);
		refreshTokenRepository.revokeAllActiveForUser(user.getId());
		writeLockLog(user.getId(), actorId, "LOCK", reason);
	}

	@Override
	@Transactional
	public void unlockAccount(UUID actorId, String username, String reason) {
		UserAccount user = findByLoginName(username);
		user.setEnabled(true);
		userAccountRepository.save(user);
		writeLockLog(user.getId(), actorId, "UNLOCK", reason);
	}

	@Override
	@Transactional(readOnly = true)
	public List<AdminUserSearchResponse> searchUsers(String name) {
		String term = name == null ? "" : name.trim();
		return userAccountRepository.searchActive(term, org.springframework.data.domain.PageRequest.of(0, 20))
				.stream()
				.map(u -> new AdminUserSearchResponse(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.isEnabled()))
				.toList();
	}

	@Override
	@Transactional
	public void recordAdminLoginSuccess(UUID userId, String username, String ipAddress, String userAgent) {
		var user = userAccountRepository.findActiveById(userId).orElse(null);
		AuthzAdminLoginLogEntity log = new AuthzAdminLoginLogEntity();
		log.setUser(user);
		log.setUsername(username);
		log.setIpAddress(ipAddress);
		log.setUserAgent(userAgent);
		log.setLoggedInAt(Instant.now());
		authzAdminLoginLogRepository.save(log);
	}

	private String createAndPersistRefreshToken(UUID userId) {
		byte[] rnd = new byte[32];
		RANDOM.nextBytes(rnd);
		String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
		String hash = sha256Hex(raw);
		RefreshTokenEntity e = new RefreshTokenEntity();
		e.setUserId(userId);
		e.setTokenHash(hash);
		e.setExpiresAt(Instant.now().plusSeconds(jwtProperties.refreshExpirationSeconds()));
		e.setRevoked(false);
		e.setCreatedAt(Instant.now());
		refreshTokenRepository.save(e);
		return raw;
	}

	private static String sha256Hex(String raw) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
