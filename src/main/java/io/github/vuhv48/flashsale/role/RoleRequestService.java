package io.github.vuhv48.flashsale.role;

import io.github.vuhv48.flashsale.auth.OrganizerApplication;
import io.github.vuhv48.flashsale.common.api.ErrorStatus;
import io.github.vuhv48.flashsale.common.exception.DomainException;
import io.github.vuhv48.flashsale.persistence.entity.OrganizerProfileEntity;
import io.github.vuhv48.flashsale.persistence.entity.RoleChangeLogEntity;
import io.github.vuhv48.flashsale.persistence.entity.RoleEntity;
import io.github.vuhv48.flashsale.persistence.entity.RoleRequestAction;
import io.github.vuhv48.flashsale.persistence.entity.RoleRequestEntity;
import io.github.vuhv48.flashsale.persistence.entity.RoleRequestStatus;
import io.github.vuhv48.flashsale.persistence.entity.UserAccount;
import io.github.vuhv48.flashsale.persistence.repository.OrganizerProfileRepository;
import io.github.vuhv48.flashsale.persistence.repository.RefreshTokenRepository;
import io.github.vuhv48.flashsale.persistence.repository.RoleChangeLogRepository;
import io.github.vuhv48.flashsale.persistence.repository.RoleEntityRepository;
import io.github.vuhv48.flashsale.persistence.repository.RoleRequestRepository;
import io.github.vuhv48.flashsale.persistence.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleRequestService {

	private static final String ORGANIZER = "ORGANIZER";
	private static final String ADMIN = "ADMIN";
	private static final String SOURCE_REQUEST = "REQUEST";
	private static final String SOURCE_DIRECT = "DIRECT";

	private final RoleEntityRepository roleEntityRepository;
	private final RoleRequestRepository roleRequestRepository;
	private final UserAccountRepository userAccountRepository;
	private final OrganizerProfileRepository organizerProfileRepository;
	private final RoleChangeLogRepository roleChangeLogRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final OpenEventQuery openEventQuery;

	@Transactional(readOnly = true)
	public List<RoleCatalogItem> listRequestable() {
		return roleEntityRepository.findByRequestableTrueAndIsDeletedFalseAndEnabledTrueOrderByNameAsc().stream()
				.map(RoleRequestService::toCatalogItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<RoleCatalogItem> listMine(UUID userId) {
		UserAccount user = activeUser(userId);
		return user.getRoles().stream()
				.filter(role -> !role.isDeleted() && role.isEnabled())
				.map(RoleRequestService::toCatalogItem)
				.toList();
	}

	@Transactional
	public String submit(UUID userId, String username, SubmitRoleRequest body) {
		UserAccount user = activeUser(userId);
		RoleEntity role = roleEntityRepository.findByCodeAndIsDeletedFalseAndEnabledTrue(body.roleCode())
				.orElseThrow(() -> new DomainException(ErrorStatus.ROLE_NOT_FOUND, ErrorStatus.ROLE_NOT_FOUND.defaultMessage()));
		if (!role.isRequestable()) {
			throw new DomainException(ErrorStatus.ROLE_NOT_REQUESTABLE, ErrorStatus.ROLE_NOT_REQUESTABLE.defaultMessage());
		}
		RoleRequestAction action = RoleRequestAction.valueOf(body.action());
		boolean holdsRole = holds(user, role.getCode());
		if (action == RoleRequestAction.ADD && holdsRole) {
			throw new DomainException(ErrorStatus.ROLE_ALREADY_ASSIGNED, ErrorStatus.ROLE_ALREADY_ASSIGNED.defaultMessage());
		}
		if (action == RoleRequestAction.REMOVE && !holdsRole) {
			throw new DomainException(ErrorStatus.ROLE_NOT_ASSIGNED, ErrorStatus.ROLE_NOT_ASSIGNED.defaultMessage());
		}
		if (action == RoleRequestAction.REMOVE && user.getRoles().size() <= 1) {
			throw new DomainException(ErrorStatus.LAST_ROLE, ErrorStatus.LAST_ROLE.defaultMessage());
		}
		if (roleRequestRepository.existsByUserIdAndRoleCodeAndStatusAndIsDeletedFalse(
				userId, role.getCode(), RoleRequestStatus.PENDING)) {
			throw new DomainException(ErrorStatus.ROLE_REQUEST_PENDING, ErrorStatus.ROLE_REQUEST_PENDING.defaultMessage());
		}
		if (action == RoleRequestAction.ADD && role.isRequiresOrgProfile()) {
			saveOrganizerProfile(userId, username, body.organizer());
		}
		RoleRequestEntity request = new RoleRequestEntity();
		request.setUserId(userId);
		request.setRoleCode(role.getCode());
		request.setAction(action);
		request.setStatus(RoleRequestStatus.PENDING);
		request.setUserNote(blankToNull(body.note()));
		request.setCreatedBy(username);
		roleRequestRepository.save(request);
		return "Đã gửi yêu cầu " + verb(action) + " vai trò " + role.getName() + ". Vui lòng chờ duyệt.";
	}

	@Transactional(readOnly = true)
	public List<RoleRequestView> history(UUID userId) {
		return roleRequestRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId).stream()
				.map(request -> toView(request, usernameOf(request.getUserId())))
				.toList();
	}

	@Transactional
	public String cancel(Long requestId, UUID userId, String username) {
		RoleRequestEntity request = pending(requestId);
		if (!request.getUserId().equals(userId)) {
			throw new DomainException(ErrorStatus.FORBIDDEN, ErrorStatus.FORBIDDEN.defaultMessage());
		}
		request.setStatus(RoleRequestStatus.CANCELLED);
		request.setUpdatedBy(username);
		roleRequestRepository.save(request);
		return "Đã hủy yêu cầu.";
	}

	@Transactional(readOnly = true)
	public List<RoleRequestView> listForAdmin(RoleRequestStatus status, String roleCode, RoleRequestAction action) {
		return roleRequestRepository.findByStatusAndIsDeletedFalseOrderByCreatedAtAsc(status).stream()
				.filter(request -> roleCode == null || roleCode.equalsIgnoreCase(request.getRoleCode()))
				.filter(request -> action == null || action == request.getAction())
				.map(request -> toView(request, usernameOf(request.getUserId())))
				.toList();
	}

	@Transactional
	public String approve(Long requestId, UUID adminId, String adminUsername) {
		RoleRequestEntity request = pending(requestId);
		UserAccount user = activeUser(request.getUserId());
		RoleEntity role = roleEntityRepository.findByCodeAndIsDeletedFalseAndEnabledTrue(request.getRoleCode())
				.orElseThrow(() -> new DomainException(ErrorStatus.ROLE_NOT_FOUND, ErrorStatus.ROLE_NOT_FOUND.defaultMessage()));
		if (request.getAction() == RoleRequestAction.REMOVE
				&& ORGANIZER.equals(role.getCode())
				&& openEventQuery.hasUnfinishedEvent(user.getId())) {
			throw new DomainException(
					ErrorStatus.ORGANIZER_HAS_OPEN_EVENTS,
					ErrorStatus.ORGANIZER_HAS_OPEN_EVENTS.defaultMessage());
		}
		applyChange(user, role, request.getAction(), adminId, SOURCE_REQUEST, request.getId(), null);
		request.setStatus(RoleRequestStatus.APPROVED);
		request.setReviewedBy(adminId);
		request.setReviewedAt(Instant.now());
		request.setUpdatedBy(adminUsername);
		roleRequestRepository.save(request);
		return "Yêu cầu " + verb(request.getAction()) + " vai trò " + role.getName() + " đã được duyệt.";
	}

	@Transactional
	public String reject(Long requestId, UUID adminId, String adminUsername, String reason) {
		RoleRequestEntity request = pending(requestId);
		RoleEntity role = roleEntityRepository.findByCodeAndIsDeletedFalseAndEnabledTrue(request.getRoleCode())
				.orElseThrow(() -> new DomainException(ErrorStatus.ROLE_NOT_FOUND, ErrorStatus.ROLE_NOT_FOUND.defaultMessage()));
		request.setStatus(RoleRequestStatus.REJECTED);
		request.setRejectReason(reason);
		request.setReviewedBy(adminId);
		request.setReviewedAt(Instant.now());
		request.setUpdatedBy(adminUsername);
		roleRequestRepository.save(request);
		return "Yêu cầu " + verb(request.getAction()) + " vai trò " + role.getName() + " bị từ chối: " + reason + ".";
	}

	@Transactional
	public String grantDirect(UUID targetUserId, UUID adminId, String roleCode, String reason) {
		UserAccount user = activeUser(targetUserId);
		RoleEntity role = roleEntityRepository.findByCodeAndIsDeletedFalseAndEnabledTrue(roleCode)
				.orElseThrow(() -> new DomainException(ErrorStatus.ROLE_NOT_FOUND, ErrorStatus.ROLE_NOT_FOUND.defaultMessage()));
		if (holds(user, role.getCode())) {
			throw new DomainException(ErrorStatus.ROLE_ALREADY_ASSIGNED, ErrorStatus.ROLE_ALREADY_ASSIGNED.defaultMessage());
		}
		applyChange(user, role, RoleRequestAction.ADD, adminId, SOURCE_DIRECT, null, reason);
		return "Đã cấp vai trò " + role.getName() + ".";
	}

	@Transactional
	public String revokeDirect(UUID targetUserId, UUID adminId, String roleCode, String reason) {
		UserAccount user = activeUser(targetUserId);
		RoleEntity role = roleEntityRepository.findByCodeAndIsDeletedFalseAndEnabledTrue(roleCode)
				.orElseThrow(() -> new DomainException(ErrorStatus.ROLE_NOT_FOUND, ErrorStatus.ROLE_NOT_FOUND.defaultMessage()));
		if (!holds(user, role.getCode())) {
			throw new DomainException(ErrorStatus.ROLE_NOT_ASSIGNED, ErrorStatus.ROLE_NOT_ASSIGNED.defaultMessage());
		}
		applyChange(user, role, RoleRequestAction.REMOVE, adminId, SOURCE_DIRECT, null, reason);
		return "Đã gỡ vai trò " + role.getName() + ".";
	}

	private void applyChange(
			UserAccount user,
			RoleEntity role,
			RoleRequestAction action,
			UUID actorId,
			String source,
			Long requestId,
			String reason
	) {
		if (action == RoleRequestAction.ADD) {
			user.getRoles().add(role);
		} else {
			ensureCanRemove(user, role, actorId);
			user.getRoles().removeIf(existing -> role.getCode().equals(existing.getCode()));
			refreshTokenRepository.revokeAllActiveForUser(user.getId());
		}
		userAccountRepository.save(user);
		RoleChangeLogEntity log = new RoleChangeLogEntity();
		log.setActorUserId(actorId);
		log.setTargetUserId(user.getId());
		log.setRoleCode(role.getCode());
		log.setAction(action.name());
		log.setSource(source);
		log.setRequestId(requestId);
		log.setReason(blankToNull(reason));
		roleChangeLogRepository.save(log);
	}

	private void ensureCanRemove(UserAccount user, RoleEntity role, UUID actorId) {
		if (user.getRoles().size() <= 1) {
			throw new DomainException(ErrorStatus.LAST_ROLE, ErrorStatus.LAST_ROLE.defaultMessage());
		}
		if (ADMIN.equals(role.getCode()) && user.getId().equals(actorId)) {
			throw new DomainException(ErrorStatus.CANNOT_REMOVE_OWN_ADMIN, ErrorStatus.CANNOT_REMOVE_OWN_ADMIN.defaultMessage());
		}
		if (ADMIN.equals(role.getCode()) && userAccountRepository.countActiveAdmins() <= 1) {
			throw new DomainException(ErrorStatus.LAST_ADMIN, ErrorStatus.LAST_ADMIN.defaultMessage());
		}
		if (ORGANIZER.equals(role.getCode()) && openEventQuery.hasUnfinishedEvent(user.getId())) {
			throw new DomainException(
					ErrorStatus.ORGANIZER_HAS_OPEN_EVENTS,
					ErrorStatus.ORGANIZER_HAS_OPEN_EVENTS.defaultMessage());
		}
	}

	private void saveOrganizerProfile(UUID userId, String username, OrganizerApplication application) {
		if (application == null) {
			throw new DomainException(ErrorStatus.VALIDATION_ERROR, "Phải nhập hồ sơ đơn vị");
		}
		if (organizerProfileRepository.existsByTaxCodeAndIsDeletedFalseAndUserIdNot(application.taxCode(), userId)) {
			throw new DomainException(ErrorStatus.TAX_CODE_TAKEN, ErrorStatus.TAX_CODE_TAKEN.defaultMessage());
		}
		OrganizerProfileEntity profile = organizerProfileRepository.findByUserIdAndIsDeletedFalse(userId)
				.orElseGet(OrganizerProfileEntity::new);
		profile.setUserId(userId);
		profile.setOrgName(application.orgName());
		profile.setTaxCode(application.taxCode());
		profile.setRepresentative(application.representative());
		if (profile.getCreatedBy() == null) {
			profile.setCreatedBy(username);
		}
		profile.setUpdatedBy(username);
		organizerProfileRepository.save(profile);
	}

	private RoleRequestEntity pending(Long requestId) {
		RoleRequestEntity request = roleRequestRepository.findByIdAndIsDeletedFalse(requestId)
				.orElseThrow(() -> new DomainException(
						ErrorStatus.ROLE_REQUEST_NOT_FOUND,
						ErrorStatus.ROLE_REQUEST_NOT_FOUND.defaultMessage()));
		if (request.getStatus() != RoleRequestStatus.PENDING) {
			throw new DomainException(
					ErrorStatus.ROLE_REQUEST_NOT_PENDING,
					ErrorStatus.ROLE_REQUEST_NOT_PENDING.defaultMessage());
		}
		return request;
	}

	private UserAccount activeUser(UUID userId) {
		UserAccount user = userAccountRepository.findActiveById(userId)
				.orElseThrow(() -> new DomainException(ErrorStatus.USER_NOT_FOUND, ErrorStatus.USER_NOT_FOUND.defaultMessage()));
		if (!user.isEnabled()) {
			throw new DomainException(ErrorStatus.ACCOUNT_DISABLED, ErrorStatus.ACCOUNT_DISABLED.defaultMessage());
		}
		return user;
	}

	private String usernameOf(UUID userId) {
		return userAccountRepository.findActiveById(userId).map(UserAccount::getUsername).orElse(null);
	}

	private RoleRequestView toView(RoleRequestEntity request, String username) {
		String roleName = roleEntityRepository.findByCodeAndIsDeletedFalseAndEnabledTrue(request.getRoleCode())
				.map(RoleEntity::getName)
				.orElse(request.getRoleCode());
		boolean blocked = request.getStatus() == RoleRequestStatus.PENDING
				&& request.getAction() == RoleRequestAction.REMOVE
				&& ORGANIZER.equals(request.getRoleCode())
				&& openEventQuery.hasUnfinishedEvent(request.getUserId());
		return new RoleRequestView(
				request.getId(),
				request.getUserId(),
				username,
				request.getRoleCode(),
				roleName,
				request.getAction().name(),
				request.getStatus().name(),
				request.getUserNote(),
				request.getRejectReason(),
				blocked,
				blocked ? ErrorStatus.ORGANIZER_HAS_OPEN_EVENTS.defaultMessage() : null,
				request.getCreatedAt(),
				request.getReviewedAt());
	}

	private static boolean holds(UserAccount user, String roleCode) {
		return user.getRoles().stream().anyMatch(role -> roleCode.equals(role.getCode()));
	}

	private static RoleCatalogItem toCatalogItem(RoleEntity role) {
		return new RoleCatalogItem(role.getCode(), role.getName(), role.getDescription(), role.isRequiresOrgProfile());
	}

	private static String verb(RoleRequestAction action) {
		return action == RoleRequestAction.ADD ? "thêm" : "gỡ";
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
