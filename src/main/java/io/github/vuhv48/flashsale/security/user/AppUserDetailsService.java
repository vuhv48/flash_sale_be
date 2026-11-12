package io.github.vuhv48.flashsale.security.user;

import io.github.vuhv48.flashsale.common.security.LoadUserForSecurityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

	private final LoadUserForSecurityPort loadUserForSecurity;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		var loaded = username != null && username.contains("@")
				? loadUserForSecurity.loadByEmail(username.trim().toLowerCase())
				: loadUserForSecurity.loadByUsername(username);
		return loaded.map(AppUserDetails::new)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}
}
