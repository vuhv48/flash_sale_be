package io.github.vuhv48.flashsale.auth;

import io.github.vuhv48.flashsale.common.api.ErrorStatus;
import io.github.vuhv48.flashsale.common.exception.DomainException;
import io.github.vuhv48.flashsale.common.security.LoadUserForSecurityPort;
import io.github.vuhv48.flashsale.persistence.entity.AuthzAdminLoginLogEntity;
import io.github.vuhv48.flashsale.persistence.entity.RefreshTokenEntity;
import io.github.vuhv48.flashsale.persistence.entity.RoleEntity;
import io.github.vuhv48.flashsale.persistence.entity.UserAccount;
import io.github.vuhv48.flashsale.persistence.repository.AuthzAdminLoginLogRepository;
import io.github.vuhv48.flashsale.persistence.repository.RefreshTokenRepository;
import io.github.vuhv48.flashsale.persistence.repository.RoleEntityRepository;
import io.github.vuhv48.flashsale.persistence.repository.UserAccountRepository;
import io.github.vuhv48.flashsale.security.config.JwtProperties;
import io.github.vuhv48.flashsale.security.jwt.JwtService;
import io.github.vuhv48.flashsale.security.user.AppUserDetails;
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

	@Override
	@Transactional
	public RegisterOutcome register(
			String username,
			String rawPassword,
			String contactEmail,
			String fullName,
			String phone
	) {
		String u = username.trim();
		if (u.isEmpty()) {
			throw new DomainException(ErrorStatus.VALIDATION_ERROR, "Tên đăng nhập không hợp lệ");
		}
		if (userAccountRepository.existsByUsername(u)) {
			throw new DomainException(ErrorStatus.USERNAME_TAKEN, ErrorStatus.USERNAME_TAKEN.defaultMessage());
		}
		String email = normalizeEmail(contactEmail);
		if (email != null && userAccountRepository.existsByEmail(email)) {
			throw new DomainException(ErrorStatus.EMAIL_TAKEN, ErrorStatus.EMAIL_TAKEN.defaultMessage());
		}
		RoleEntity userRole = roleEntityRepository.findByCodeAndIsDeletedFalseAndEnabledTrue(DEFAULT_SELF_REGISTER_ROLE)
				.orElseThrow(() -> new DomainException(
						ErrorStatus.ILLEGAL_STATE,
						"Chưa cấu hình role CUSTOMER trong CSDL. Chạy migration Flyway."));
		UserAccount acc = new UserAccount();
		acc.setUsername(u);
		acc.setEmail(email);
		acc.setFullName(blankToNull(fullName));
		acc.setPhone(blankToNull(phone));
		acc.setPasswordHash(passwordEncoder.encode(rawPassword));
		acc.setEnabled(true);
		acc.setDataScope("OWN");
		acc.getRoles().add(userRole);
		userAccountRepository.save(acc);
		registerWelcomeMailService.enqueueWelcomeEmail(acc.getId(), u, email);
		registerWelcomeMailService.enqueueAdminNewUserNotification(acc.getId(), u, email);
		var snap = loadUserForSecurity.loadByUsername(u)
				.orElseThrow(() -> new DomainException(ErrorStatus.INTERNAL_ERROR, ErrorStatus.INTERNAL_ERROR.defaultMessage()));
		return new RegisterOutcome(issueTokens(new AppUserDetails(snap)), "Đăng ký thành công.");
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
	public void setUserEnabledByUsername(String username, boolean enabled) {
		UserAccount u = userAccountRepository.findByUsername(username.trim())
				.orElseThrow(() -> new DomainException(ErrorStatus.USER_NOT_FOUND, ErrorStatus.USER_NOT_FOUND.defaultMessage()));
		u.setEnabled(enabled);
		userAccountRepository.save(u);
		if (!enabled) {
			refreshTokenRepository.revokeAllActiveForUser(u.getId());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<AdminUserSearchResponse> searchUsersByName(String name) {
		if (name == null || name.isBlank()) {
			return List.of();
		}
		return userAccountRepository.findTop20ByIsDeletedFalseAndUsernameContainingIgnoreCaseOrderByUsernameAsc(name.trim())
				.stream()
				.map(u -> new AdminUserSearchResponse(
						u.getId(),
						u.getUsername(),
						u.isEnabled(),
						u.getDataScope()))
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
