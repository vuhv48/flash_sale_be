package io.github.vuhv48.flashsale.persistence.repository;

import java.util.UUID;

public interface UserSecurityCredentialsProjection {

	UUID getId();

	String getUsername();

	String getPasswordHash();

	boolean isEnabled();

	String getDataScope();
}
