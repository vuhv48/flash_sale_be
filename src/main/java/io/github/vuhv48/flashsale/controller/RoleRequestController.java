package io.github.vuhv48.flashsale.controller;

import io.github.vuhv48.flashsale.auth.MessageResponse;
import io.github.vuhv48.flashsale.common.api.ErrorStatus;
import io.github.vuhv48.flashsale.common.exception.DomainException;
import io.github.vuhv48.flashsale.organizer.RejectOrganizerRequest;
import io.github.vuhv48.flashsale.persistence.entity.RoleRequestAction;
import io.github.vuhv48.flashsale.persistence.entity.RoleRequestStatus;
import io.github.vuhv48.flashsale.role.GrantRoleRequest;
import io.github.vuhv48.flashsale.role.MyPermission;
import io.github.vuhv48.flashsale.role.RoleCatalogItem;
import io.github.vuhv48.flashsale.role.RoleRequestView;
import io.github.vuhv48.flashsale.role.SubmitRoleRequest;
import io.github.vuhv48.flashsale.security.user.AppUserDetails;
import io.github.vuhv48.flashsale.service.MePermissionService;
import io.github.vuhv48.flashsale.service.RoleRequestService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Vai trò", description = "Xin thêm hoặc gỡ vai trò, và xem quyền của tài khoản đang đăng nhập")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoleRequestController {

	private static final String ROLE_EXAMPLE = """
			[
			  {
			    "code": "CUSTOMER",
			    "name": "Khách hàng",
			    "description": "Tự có khi đăng ký",
			    "requiresOrgProfile": false
			  },
			  {
			    "code": "ORGANIZER",
			    "name": "Ban tổ chức",
			    "description": "Xin thêm trong hồ sơ, kèm hồ sơ đơn vị, Admin duyệt",
			    "requiresOrgProfile": true
			  }
			]
			""";

	private static final String REQUEST_EXAMPLE = """
			[
			  {
			    "id": 12,
			    "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
			    "username": "user",
			    "roleCode": "ORGANIZER",
			    "roleName": "Ban tổ chức",
			    "action": "ADD",
			    "status": "PENDING",
			    "note": "Công ty tổ chức concert",
			    "rejectReason": null,
			    "approveBlocked": false,
			    "approveBlockReason": null,
			    "createdAt": "2026-11-10T07:50:12Z",
			    "reviewedAt": null
			  }
			]
			""";

	private static final String PERMISSION_EXAMPLE = """
			[
			  {
			    "code": "PER-04",
			    "name": "Xem, sửa hồ sơ; đổi mật khẩu; làm mới token; đăng xuất",
			    "group": "ACCOUNT",
			    "action": "MANAGE_PROFILE",
			    "accessLevel": "OWN"
			  }
			]
			""";

	private final RoleRequestService roleRequestService;
	private final MePermissionService mePermissionService;

	@Operation(
			summary = "Danh sách vai trò được phép xin",
			description = "Các vai trò người dùng được tự xin thêm hoặc gỡ. ADMIN không nằm trong danh sách."
	)
	@ApiResponse(responseCode = "200", description = "Vai trò được phép xin",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoleCatalogItem.class)),
					examples = @ExampleObject(value = ROLE_EXAMPLE)))
	@GetMapping("/roles/requestable")
	public List<RoleCatalogItem> requestable() {
		return roleRequestService.listRequestable();
	}

	@Operation(
			summary = "Vai trò hiện có của tôi",
			description = "Các vai trò đang gán cho tài khoản đăng nhập. Đăng ký xong thì có CUSTOMER."
	)
	@ApiResponse(responseCode = "200", description = "Vai trò đang có",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoleCatalogItem.class)),
					examples = @ExampleObject(value = """
							[
							  {
							    "code": "CUSTOMER",
							    "name": "Khách hàng",
							    "description": "Tự có khi đăng ký",
							    "requiresOrgProfile": false
							  }
							]
							""")))
	@GetMapping("/me/roles")
	public List<RoleCatalogItem> mine(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal) {
		return roleRequestService.listMine(principal.getId());
	}

	@Operation(
			summary = "Quyền hiệu lực của tôi",
			description = "Quyền dùng để ẩn hiện chức năng trên giao diện. Lấy từ mọi vai trò đang có, cộng quyền gán thẳng, trừ quyền bị cấm. "
					+ "Trùng một mã thì giữ mức rộng hơn: ALL hơn OWN, OWN hơn AUTO."
	)
	@ApiResponse(responseCode = "200", description = "Quyền hiệu lực",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = MyPermission.class)),
					examples = @ExampleObject(value = PERMISSION_EXAMPLE)))
	@GetMapping("/me/permissions")
	public List<MyPermission> permissions(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal) {
		return mePermissionService.listMine(principal.getId());
	}

	@Operation(
			summary = "Gửi yêu cầu thêm hoặc gỡ vai trò",
			description = "Tạo một yêu cầu PENDING. Vai trò chỉ đổi khi Admin duyệt. "
					+ "Xin thêm vai trò có hồ sơ đơn vị thì phải gửi organizer. Mỗi vai trò chỉ có một yêu cầu đang chờ."
	)
	@ApiResponse(responseCode = "201", description = "Đã gửi yêu cầu",
			content = @Content(schema = @Schema(implementation = MessageResponse.class),
					examples = @ExampleObject(value = "{\"message\":\"Đã gửi yêu cầu thêm vai trò Ban tổ chức. Vui lòng chờ duyệt.\"}")))
	@PostMapping("/role-requests")
	public ResponseEntity<MessageResponse> submit(
			@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody SubmitRoleRequest body
	) {
		String message = roleRequestService.submit(principal.getId(), principal.getUsername(), body);
		return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse(message));
	}

	@Operation(
			summary = "Xem lịch sử yêu cầu vai trò của tôi",
			description = "Các yêu cầu thêm hoặc gỡ vai trò do chính tài khoản đang đăng nhập đã gửi, mới nhất trước. "
					+ "Gồm yêu cầu đang chờ, đã duyệt, bị từ chối và đã hủy. API này chỉ để xem, không đổi quyền. "
					+ "Chưa gửi yêu cầu nào thì danh sách rỗng."
	)
	@ApiResponse(responseCode = "200", description = "Lịch sử yêu cầu của tôi",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoleRequestView.class)),
					examples = @ExampleObject(name = "Đang chờ duyệt ban tổ chức", value = REQUEST_EXAMPLE)))
	@GetMapping("/role-requests/me")
	public List<RoleRequestView> history(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal) {
		return roleRequestService.history(principal.getId());
	}

	@Operation(
			summary = "Hủy yêu cầu đang chờ",
			description = "Người gửi tự hủy yêu cầu khi trạng thái còn PENDING. Yêu cầu chuyển sang CANCELLED."
	)
	@ApiResponse(responseCode = "200", description = "Đã hủy",
			content = @Content(schema = @Schema(implementation = MessageResponse.class),
					examples = @ExampleObject(value = "{\"message\":\"Đã hủy yêu cầu.\"}")))
	@PostMapping("/role-requests/{id}/cancel")
	public MessageResponse cancel(
			@Parameter(description = "Mã yêu cầu", example = "12") @PathVariable Long id,
			@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal
	) {
		return new MessageResponse(roleRequestService.cancel(id, principal.getId(), principal.getUsername()));
	}

	@Operation(
			summary = "Admin xem danh sách yêu cầu vai trò",
			description = "Chỉ tài khoản có vai trò ADMIN. Mặc định lấy yêu cầu PENDING, cũ nhất trước. Có thể lọc theo mã vai trò và ADD hoặc REMOVE."
	)
	@ApiResponse(responseCode = "200", description = "Danh sách yêu cầu",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoleRequestView.class)),
					examples = @ExampleObject(value = REQUEST_EXAMPLE)))
	@GetMapping("/admin/role-requests")
	@PreAuthorize("hasRole('ADMIN')")
	public List<RoleRequestView> list(
			@Parameter(description = "PENDING, APPROVED, REJECTED hoặc CANCELLED", example = "PENDING")
			@RequestParam(defaultValue = "PENDING") String status,
			@Parameter(description = "Lọc theo mã vai trò", example = "ORGANIZER")
			@RequestParam(required = false) String roleCode,
			@Parameter(description = "ADD hoặc REMOVE", example = "ADD")
			@RequestParam(required = false) String action
	) {
		return roleRequestService.listForAdmin(parseStatus(status), blankToNull(roleCode), parseAction(action));
	}

	@Operation(
			summary = "Admin duyệt yêu cầu vai trò",
			description = "Thêm hoặc gỡ đúng vai trò trong yêu cầu. Thêm thì giữ các vai trò còn lại. "
					+ "Gỡ thì thu hồi refresh token. Không duyệt gỡ ORGANIZER khi người đó còn sự kiện chưa kết thúc."
	)
	@ApiResponse(responseCode = "200", description = "Đã duyệt",
			content = @Content(schema = @Schema(implementation = MessageResponse.class),
					examples = @ExampleObject(value = "{\"message\":\"Yêu cầu thêm vai trò Ban tổ chức đã được duyệt.\"}")))
	@PostMapping("/admin/role-requests/{id}/approve")
	@PreAuthorize("hasRole('ADMIN')")
	public MessageResponse approve(
			@Parameter(description = "Mã yêu cầu", example = "12") @PathVariable Long id,
			@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal
	) {
		return new MessageResponse(roleRequestService.approve(id, principal.getId(), principal.getUsername()));
	}

	@Operation(
			summary = "Admin từ chối yêu cầu vai trò",
			description = "Từ chối yêu cầu đang chờ. Lý do từ 10 đến 500 ký tự. Vai trò của người gửi không đổi."
	)
	@ApiResponse(responseCode = "200", description = "Đã từ chối",
			content = @Content(schema = @Schema(implementation = MessageResponse.class),
					examples = @ExampleObject(value = "{\"message\":\"Yêu cầu thêm vai trò Ban tổ chức bị từ chối: Thiếu giấy tờ đăng ký kinh doanh.\"}")))
	@PostMapping("/admin/role-requests/{id}/reject")
	@PreAuthorize("hasRole('ADMIN')")
	public MessageResponse reject(
			@Parameter(description = "Mã yêu cầu", example = "12") @PathVariable Long id,
			@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody RejectOrganizerRequest body
	) {
		return new MessageResponse(roleRequestService.reject(id, principal.getId(), principal.getUsername(), body.reason()));
	}

	@Operation(
			summary = "Admin cấp thẳng vai trò",
			description = "Cấp một vai trò không qua yêu cầu, kể cả ADMIN. Các vai trò đang có được giữ nguyên."
	)
	@ApiResponse(responseCode = "200", description = "Đã cấp vai trò",
			content = @Content(schema = @Schema(implementation = MessageResponse.class),
					examples = @ExampleObject(value = "{\"message\":\"Đã cấp vai trò Quản trị viên.\"}")))
	@PostMapping("/admin/users/{userId}/roles")
	@PreAuthorize("hasRole('ADMIN')")
	public MessageResponse grant(
			@Parameter(description = "Mã người nhận vai trò", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
			@PathVariable UUID userId,
			@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody GrantRoleRequest body
	) {
		return new MessageResponse(roleRequestService.grantDirect(
				userId, principal.getId(), body.roleCode(), body.reason()));
	}

	@Operation(
			summary = "Admin gỡ thẳng vai trò",
			description = "Gỡ một vai trò không qua yêu cầu. Tài khoản phải còn ít nhất một vai trò. "
					+ "Admin không tự gỡ ADMIN của mình và không gỡ quản trị viên cuối cùng. Gỡ vai trò thì thu hồi refresh token."
	)
	@ApiResponse(responseCode = "200", description = "Đã gỡ vai trò",
			content = @Content(schema = @Schema(implementation = MessageResponse.class),
					examples = @ExampleObject(value = "{\"message\":\"Đã gỡ vai trò Ban tổ chức.\"}")))
	@DeleteMapping("/admin/users/{userId}/roles/{code}")
	@PreAuthorize("hasRole('ADMIN')")
	public MessageResponse revoke(
			@Parameter(description = "Mã người bị gỡ vai trò", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
			@PathVariable UUID userId,
			@Parameter(description = "Mã vai trò cần gỡ", example = "ORGANIZER")
			@PathVariable String code,
			@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
			@Parameter(description = "Lý do, không bắt buộc", example = "Người này không còn tổ chức sự kiện")
			@RequestParam(required = false) String reason
	) {
		return new MessageResponse(roleRequestService.revokeDirect(userId, principal.getId(), code, reason));
	}

	private static RoleRequestStatus parseStatus(String status) {
		try {
			return RoleRequestStatus.valueOf(status.trim().toUpperCase());
		} catch (RuntimeException ex) {
			throw new DomainException(ErrorStatus.VALIDATION_ERROR, "Trạng thái không hợp lệ");
		}
	}

	private static RoleRequestAction parseAction(String action) {
		if (action == null || action.isBlank()) {
			return null;
		}
		try {
			return RoleRequestAction.valueOf(action.trim().toUpperCase());
		} catch (RuntimeException ex) {
			throw new DomainException(ErrorStatus.VALIDATION_ERROR, "Loại yêu cầu không hợp lệ");
		}
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
