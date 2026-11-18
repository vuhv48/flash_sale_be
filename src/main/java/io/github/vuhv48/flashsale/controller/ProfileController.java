package io.github.vuhv48.flashsale.controller;

import io.github.vuhv48.flashsale.auth.ProfileResponse;
import io.github.vuhv48.flashsale.auth.UpdateProfileRequest;
import io.github.vuhv48.flashsale.security.user.AppUserDetails;
import io.github.vuhv48.flashsale.service.AuthAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Hồ sơ", description = "Xem và sửa hồ sơ của tài khoản đang đăng nhập")
@RestController
@RequestMapping("/api/me/profile")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PER-04')")
public class ProfileController {

	private final AuthAccountService authAccountService;

	@Operation(
			summary = "Xem hồ sơ của tôi",
			description = "Trả về email, họ tên, số điện thoại và trạng thái khóa. Email không đổi sau khi đăng ký. Ảnh đại diện thuộc module tệp."
	)
	@ApiResponse(responseCode = "200", description = "Hồ sơ",
			content = @Content(schema = @Schema(implementation = ProfileResponse.class), examples = @ExampleObject(value = """
					{
					  "email": "vu@example.com",
					  "fullName": "Vũ Hoàng",
					  "phone": "0912345678",
					  "enabled": true
					}
					""")))
	@GetMapping
	public ProfileResponse me(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal) {
		return authAccountService.profile(principal.getId());
	}

	@Operation(summary = "Sửa hồ sơ của tôi", description = "Sửa họ tên và số điện thoại. Không đổi email.")
	@ApiResponse(responseCode = "200", description = "Hồ sơ sau khi sửa",
			content = @Content(schema = @Schema(implementation = ProfileResponse.class), examples = @ExampleObject(value = """
					{
					  "email": "vu@example.com",
					  "fullName": "Vũ Hoàng",
					  "phone": "0987654321",
					  "enabled": true
					}
					""")))
	@PutMapping
	public ProfileResponse update(
			@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody UpdateProfileRequest body
	) {
		return authAccountService.updateProfile(principal.getId(), body.fullName(), body.phone());
	}
}
