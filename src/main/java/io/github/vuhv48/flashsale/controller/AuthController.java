package io.github.vuhv48.flashsale.controller;

import io.github.vuhv48.flashsale.auth.AccessRefreshResult;
import io.github.vuhv48.flashsale.auth.ChangePasswordRequest;
import io.github.vuhv48.flashsale.auth.LoginRequest;
import io.github.vuhv48.flashsale.auth.LoginResponse;
import io.github.vuhv48.flashsale.auth.MessageResponse;
import io.github.vuhv48.flashsale.auth.RefreshTokenRequest;
import io.github.vuhv48.flashsale.auth.RegisterRequest;
import io.github.vuhv48.flashsale.auth.RegisterResponse;
import io.github.vuhv48.flashsale.common.api.ErrorStatus;
import io.github.vuhv48.flashsale.common.exception.DomainException;
import io.github.vuhv48.flashsale.security.user.AppUserDetails;
import io.github.vuhv48.flashsale.service.AuthAccountService;
import io.github.vuhv48.flashsale.service.LoginRateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Xác thực", description = "Đăng ký, đăng nhập, phiên làm việc và mật khẩu")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final AuthAccountService authAccountService;
	private final LoginRateLimitService loginRateLimitService;

	@SecurityRequirements
	@Operation(
			summary = "Đăng ký tài khoản",
			description = "Tạo tài khoản mới và tự gán vai trò CUSTOMER. Không chọn vai trò trên form. "
					+ "Trả về thông báo cùng access token và refresh token."
	)
	@ApiResponse(responseCode = "201", description = "Đăng ký thành công",
			content = @Content(schema = @Schema(implementation = RegisterResponse.class), examples = @ExampleObject(value = """
					{
					  "message": "Đăng ký thành công.",
					  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ2dWhvYW5nIn0.demo",
					  "refreshToken": "dGhpcy1pcy1hLWRlbW8tcmVmcmVzaC10b2tlbg",
					  "tokenType": "Bearer",
					  "expiresInSeconds": 86400,
					  "refreshExpiresInSeconds": 604800
					}
					""")))
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

	@SecurityRequirements
	@Operation(
			summary = "Đăng nhập",
			description = "Đăng nhập bằng username và mật khẩu. Đúng thì cấp cặp token mới và thu hồi refresh token cũ. "
					+ "Sai quá 5 lần trong 1 phút từ cùng một IP thì bị chặn."
	)
	@ApiResponse(responseCode = "200", description = "Đăng nhập thành công",
			content = @Content(schema = @Schema(implementation = LoginResponse.class), examples = @ExampleObject(value = """
					{
					  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ2dWhvYW5nIn0.demo",
					  "refreshToken": "dGhpcy1pcy1hLWRlbW8tcmVmcmVzaC10b2tlbg",
					  "tokenType": "Bearer",
					  "expiresInSeconds": 86400,
					  "refreshExpiresInSeconds": 604800
					}
					""")))
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

	@SecurityRequirements
	@Operation(
			summary = "Làm mới token",
			description = "Đổi một refresh token còn hạn lấy cặp token mới. Refresh token vừa dùng bị thu hồi."
	)
	@ApiResponse(responseCode = "200", description = "Cặp token mới",
			content = @Content(schema = @Schema(implementation = LoginResponse.class), examples = @ExampleObject(value = """
					{
					  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ2dWhvYW5nIn0.demo",
					  "refreshToken": "bmV3LXJlZnJlc2gtdG9rZW4tZGVtbw",
					  "tokenType": "Bearer",
					  "expiresInSeconds": 86400,
					  "refreshExpiresInSeconds": 604800
					}
					""")))
	@PostMapping("/refresh")
	public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest body) {
		var tokens = authAccountService.rotateWithRefreshToken(body.refreshToken());
		return ResponseEntity.ok(toLoginResponse(tokens));
	}

	@Operation(
			summary = "Đăng xuất",
			description = "Thu hồi mọi refresh token đang hoạt động của tài khoản. Access token đã cấp vẫn dùng được đến khi hết hạn."
	)
	@ApiResponse(responseCode = "200", description = "Đã đăng xuất",
			content = @Content(schema = @Schema(implementation = MessageResponse.class),
					examples = @ExampleObject(value = "{\"message\":\"Đăng xuất thành công\"}")))
	@PostMapping("/logout")
	public ResponseEntity<MessageResponse> logout(
			@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal
	) {
		authAccountService.logout(principal.getId());
		return ResponseEntity.ok(new MessageResponse("Đăng xuất thành công"));
	}

	@Operation(
			summary = "Đổi mật khẩu",
			description = "Đổi mật khẩu khi nhập đúng mật khẩu hiện tại. Mọi refresh token đang hoạt động bị thu hồi."
	)
	@ApiResponse(responseCode = "200", description = "Đã đổi mật khẩu",
			content = @Content(schema = @Schema(implementation = MessageResponse.class),
					examples = @ExampleObject(value = "{\"message\":\"Đổi mật khẩu thành công\"}")))
	@PostMapping("/change-password")
	public ResponseEntity<MessageResponse> changePassword(
			@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
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
