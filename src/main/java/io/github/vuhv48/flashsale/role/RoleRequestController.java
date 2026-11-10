package io.github.vuhv48.flashsale.role;

import io.github.vuhv48.flashsale.auth.MessageResponse;
import io.github.vuhv48.flashsale.organizer.RejectOrganizerRequest;
import io.github.vuhv48.flashsale.common.api.ErrorStatus;
import io.github.vuhv48.flashsale.common.exception.DomainException;
import io.github.vuhv48.flashsale.persistence.entity.RoleRequestAction;
import io.github.vuhv48.flashsale.persistence.entity.RoleRequestStatus;
import io.github.vuhv48.flashsale.security.user.AppUserDetails;
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

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoleRequestController {

	private final RoleRequestService roleRequestService;
	private final MePermissionService mePermissionService;

	@GetMapping("/roles/requestable")
	public List<RoleCatalogItem> requestable() {
		return roleRequestService.listRequestable();
	}

	@GetMapping("/me/roles")
	public List<RoleCatalogItem> mine(@AuthenticationPrincipal AppUserDetails principal) {
		return roleRequestService.listMine(principal.getId());
	}

	@GetMapping("/me/permissions")
	public List<MyPermission> permissions(@AuthenticationPrincipal AppUserDetails principal) {
		return mePermissionService.listMine(principal.getId());
	}

	@PostMapping("/role-requests")
	public ResponseEntity<MessageResponse> submit(
			@AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody SubmitRoleRequest body
	) {
		String message = roleRequestService.submit(principal.getId(), principal.getUsername(), body);
		return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse(message));
	}

	@GetMapping("/role-requests/me")
	public List<RoleRequestView> history(@AuthenticationPrincipal AppUserDetails principal) {
		return roleRequestService.history(principal.getId());
	}

	@PostMapping("/role-requests/{id}/cancel")
	public MessageResponse cancel(
			@PathVariable Long id,
			@AuthenticationPrincipal AppUserDetails principal
	) {
		return new MessageResponse(roleRequestService.cancel(id, principal.getId(), principal.getUsername()));
	}

	@GetMapping("/admin/role-requests")
	@PreAuthorize("hasRole('ADMIN')")
	public List<RoleRequestView> list(
			@RequestParam(defaultValue = "PENDING") String status,
			@RequestParam(required = false) String roleCode,
			@RequestParam(required = false) String action
	) {
		return roleRequestService.listForAdmin(parseStatus(status), blankToNull(roleCode), parseAction(action));
	}

	@PostMapping("/admin/role-requests/{id}/approve")
	@PreAuthorize("hasRole('ADMIN')")
	public MessageResponse approve(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails principal) {
		return new MessageResponse(roleRequestService.approve(id, principal.getId(), principal.getUsername()));
	}

	@PostMapping("/admin/role-requests/{id}/reject")
	@PreAuthorize("hasRole('ADMIN')")
	public MessageResponse reject(
			@PathVariable Long id,
			@AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody RejectOrganizerRequest body
	) {
		return new MessageResponse(roleRequestService.reject(id, principal.getId(), principal.getUsername(), body.reason()));
	}

	@PostMapping("/admin/users/{userId}/roles")
	@PreAuthorize("hasRole('ADMIN')")
	public MessageResponse grant(
			@PathVariable UUID userId,
			@AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody GrantRoleRequest body
	) {
		return new MessageResponse(roleRequestService.grantDirect(
				userId, principal.getId(), body.roleCode(), body.reason()));
	}

	@DeleteMapping("/admin/users/{userId}/roles/{code}")
	@PreAuthorize("hasRole('ADMIN')")
	public MessageResponse revoke(
			@PathVariable UUID userId,
			@PathVariable String code,
			@AuthenticationPrincipal AppUserDetails principal,
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
