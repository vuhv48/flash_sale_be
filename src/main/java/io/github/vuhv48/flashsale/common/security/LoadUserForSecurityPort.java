package io.github.vuhv48.flashsale.common.security;

import java.util.Optional;
import java.util.UUID;

public interface LoadUserForSecurityPort {

	Optional<UserSecuritySnapshot> loadByUsername(String username);

	Optional<UserSecuritySnapshot> loadByEmail(String email);

	Optional<UserSecuritySnapshot> loadByUserId(UUID userId);
}
