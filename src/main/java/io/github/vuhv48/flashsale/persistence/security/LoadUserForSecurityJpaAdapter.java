package io.github.vuhv48.flashsale.persistence.security;

import io.github.vuhv48.flashsale.common.security.LoadUserForSecurityPort;
import io.github.vuhv48.flashsale.common.security.UserSecuritySnapshot;
import io.github.vuhv48.flashsale.persistence.repository.UserAccountRepository;
import io.github.vuhv48.flashsale.persistence.repository.UserSecurityCredentialsProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoadUserForSecurityJpaAdapter implements LoadUserForSecurityPort {

	private final UserAccountRepository users;

	@Override
	@Transactional(readOnly = true)
	public Optional<UserSecuritySnapshot> loadByUsername(String username) {
		return users.findActiveCredentialsByUsername(username).map(this::toSnapshot);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UserSecuritySnapshot> loadByUserId(UUID userId) {
		return users.findActiveCredentialsById(userId).map(this::toSnapshot);
	}

	private UserSecuritySnapshot toSnapshot(UserSecurityCredentialsProjection c) {
		UUID id = c.getId();
		var roleCodes = new LinkedHashSet<>(users.findActiveRoleCodesByUserId(id));
		var perms = new LinkedHashSet<>(users.findActivePermissionCodesViaRolesByUserId(id));
		for (String p : users.findActiveDirectGrantedPermissionCodesByUserId(id)) {
			perms.add(p);
		}
		for (String p : users.findActiveDirectDeniedPermissionCodesByUserId(id)) {
			perms.remove(p);
		}
		return new UserSecuritySnapshot(
				id,
				c.getUsername(),
				c.getPasswordHash(),
				c.isEnabled(),
				c.getDataScope(),
				roleCodes,
				perms);
	}
}
