package io.github.vuhv48.flashsale.role;

import io.github.vuhv48.flashsale.persistence.repository.EffectivePermissionRow;
import io.github.vuhv48.flashsale.persistence.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MePermissionService {

	private final UserAccountRepository userAccountRepository;

	@Transactional(readOnly = true)
	public List<MyPermission> listMine(UUID userId) {
		Map<String, MyPermission> byCode = new LinkedHashMap<>();
		for (EffectivePermissionRow row : userAccountRepository.findRolePermissionRowsByUserId(userId)) {
			byCode.merge(row.getCode(), toPermission(row), MePermissionService::wider);
		}
		for (EffectivePermissionRow row : userAccountRepository.findDirectGrantedPermissionRowsByUserId(userId)) {
			byCode.putIfAbsent(row.getCode(), toPermission(row));
		}
		for (String denied : userAccountRepository.findActiveDirectDeniedPermissionCodesByUserId(userId)) {
			byCode.remove(denied);
		}
		return byCode.values().stream()
				.sorted(Comparator.comparing(MyPermission::code))
				.toList();
	}

	private static MyPermission wider(MyPermission current, MyPermission incoming) {
		return rank(incoming.accessLevel()) > rank(current.accessLevel()) ? incoming : current;
	}

	private static int rank(String accessLevel) {
		if (accessLevel == null) {
			return 0;
		}
		return switch (accessLevel) {
			case "ALL" -> 3;
			case "OWN" -> 2;
			case "AUTO" -> 1;
			default -> 0;
		};
	}

	private static MyPermission toPermission(EffectivePermissionRow row) {
		return new MyPermission(
				row.getCode(),
				row.getName(),
				row.getPermissionGroup(),
				row.getActionCode(),
				row.getAccessLevel());
	}
}
