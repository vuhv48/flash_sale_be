package io.github.vuhv48.flashsale.controller;

import io.github.vuhv48.flashsale.auth.AccountActionRequest;
import io.github.vuhv48.flashsale.auth.AdminUserSearchResponse;
import io.github.vuhv48.flashsale.auth.MessageResponse;
import io.github.vuhv48.flashsale.security.user.AppUserDetails;
import io.github.vuhv48.flashsale.service.AuthAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Quản trị tài khoản", description = "Admin xem, khóa và mở khóa tài khoản")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

	private final AuthAccountService authAccountService;

	@Operation(summary = "Tìm tài khoản", description = "Tối đa 20 tài khoản chưa xóa, lọc theo email, username hoặc họ tên. Bỏ trống thì lấy 20 tài khoản đầu.")
	@ApiResponse(responseCode = "200", description = "Danh sách tài khoản",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = AdminUserSearchResponse.class)),
					examples = @ExampleObject(value = """
							[
							  {
							    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
							    "email": "vu@example.com",
							    "fullName": "Vũ Hoàng",
							    "phone": "0912345678",
							    "enabled": true
							  }
							]
							""")))
	@GetMapping
	@PreAuthorize("hasAuthority('READ_USERS')")
	public List<AdminUserSearchResponse> search(
			@Parameter(description = "Chuỗi tìm, không bắt buộc", example = "vu")
			@RequestParam(required = false) String name
	) {
		return authAccountService.searchUsers(name);
	}

	@Operation(
			summary = "Khóa tài khoản",
			description = "Admin khóa tài khoản và thu hồi refresh token. Request tiếp theo của tài khoản đó nhận 401. Admin không tự khóa mình."
	)
	@ApiResponse(responseCode = "200", description = "Đã khóa",
			content = @Content(schema = @Schema(implementation = MessageResponse.class),
					examples = @ExampleObject(value = "{\"message\":\"Đã khóa tài khoản.\"}")))
	@PostMapping("/{username}/lock")
	@PreAuthorize("hasAuthority('LOCK_ACCOUNT')")
	public MessageResponse lock(
			@Parameter(description = "Username. Tài khoản đăng ký mới có username trùng email.", example = "vu@example.com")
			@PathVariable String username,
			@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody AccountActionRequest body
	) {
		authAccountService.lockAccount(principal.getId(), username, body.reason());
		return new MessageResponse("Đã khóa tài khoản.");
	}

	@Operation(summary = "Mở khóa tài khoản", description = "Admin mở khóa tài khoản đã khóa. Bắt buộc có lý do.")
	@ApiResponse(responseCode = "200", description = "Đã mở khóa",
			content = @Content(schema = @Schema(implementation = MessageResponse.class),
					examples = @ExampleObject(value = "{\"message\":\"Đã mở khóa tài khoản.\"}")))
	@PostMapping("/{username}/unlock")
	@PreAuthorize("hasAuthority('LOCK_ACCOUNT')")
	public MessageResponse unlock(
			@Parameter(example = "vu@example.com") @PathVariable String username,
			@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody AccountActionRequest body
	) {
		authAccountService.unlockAccount(principal.getId(), username, body.reason());
		return new MessageResponse("Đã mở khóa tài khoản.");
	}
}
