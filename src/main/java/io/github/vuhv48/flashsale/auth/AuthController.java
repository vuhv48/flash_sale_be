package io.github.vuhv48.flashsale.auth;

import io.github.vuhv48.flashsale.common.api.ErrorStatus;
import io.github.vuhv48.flashsale.common.exception.DomainException;
import io.github.vuhv48.flashsale.security.user.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final AuthAccountService authAccountService;
	private final LoginRateLimitService loginRateLimitService;

	@PostMapping("/register")
	public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest body) {
		var result = authAccountService.register(
				body.username(),
				body.password(),
				body.email(),
				body.fullName(),
				body.phone());
		var tokens = result.tokens();
		return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponse(
				result.message(),
				tokens.accessToken(),
				tokens.refreshToken(),
				"Bearer",
				tokens.accessExpiresInSeconds(),
				tokens.refreshExpiresInSeconds()));
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
		String clientIp = resolveClientIp(request);
		if (!loginRateLimitService.isAllowed(clientIp)) {
			throw new DomainException(
					ErrorStatus.TOO_MANY_REQUESTS,
					"Bạn đã vượt quá 5 lần đăng nhập trong 1 phút từ IP: " + clientIp
			);
		}
		var auth = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
		var principal = (AppUserDetails) auth.getPrincipal();
		var tokens = authAccountService.issueTokens(principal);
		loginRateLimitService.clear(clientIp);
		authAccountService.recordAdminLoginSuccess(
				principal.getId(),
				principal.getUsername(),
				request.getRemoteAddr(),
				request.getHeader("User-Agent"));
		return ResponseEntity.ok(toLoginResponse(tokens));
	}

	@PostMapping("/refresh")
	public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest body) {
		var tokens = authAccountService.rotateWithRefreshToken(body.refreshToken());
		return ResponseEntity.ok(toLoginResponse(tokens));
	}

	@PostMapping("/logout")
	public ResponseEntity<MessageResponse> logout(@AuthenticationPrincipal AppUserDetails principal) {
		authAccountService.logout(principal.getId());
		return ResponseEntity.ok(new MessageResponse("Đăng xuất thành công"));
	}

	@PostMapping("/change-password")
	public ResponseEntity<MessageResponse> changePassword(
			@AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody ChangePasswordRequest body
	) {
		authAccountService.changePassword(principal.getId(), body.currentPassword(), body.newPassword());
		return ResponseEntity.ok(new MessageResponse("Đổi mật khẩu thành công"));
	}

	private static LoginResponse toLoginResponse(AccessRefreshResult r) {
		return new LoginResponse(
				r.accessToken(),
				r.refreshToken(),
				"Bearer",
				r.accessExpiresInSeconds(),
				r.refreshExpiresInSeconds());
	}

	private static String resolveClientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			String[] parts = forwardedFor.split(",");
			if (parts.length > 0 && !parts[0].isBlank()) {
				return parts[0].trim();
			}
		}
		String realIp = request.getHeader("X-Real-IP");
		if (realIp != null && !realIp.isBlank()) {
			return realIp.trim();
		}
		return request.getRemoteAddr();
	}
}
