package io.github.vuhv48.flashsale.persistence.repository;

import io.github.vuhv48.flashsale.persistence.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

	@Query("select case when count(u) > 0 then true else false end from UserAccount u where u.username = :username and u.isDeleted = false")
	boolean existsByUsername(@Param("username") String username);

	@Query("select case when count(u) > 0 then true else false end from UserAccount u where lower(u.email) = :email")
	boolean existsByEmail(@Param("email") String email);

	@Query("select u from UserAccount u where u.username = :username and u.isDeleted = false")
	Optional<UserAccount> findByUsername(@Param("username") String username);

	@Query("select u from UserAccount u where u.id = :id and u.isDeleted = false")
	Optional<UserAccount> findActiveById(@Param("id") UUID id);

	List<UserAccount> findTop20ByIsDeletedFalseAndUsernameContainingIgnoreCaseOrderByUsernameAsc(String username);

	@Query(
			value = """
					select u.id as id, u.username as username, u.password_hash as passwordHash,
					       u.enabled as enabled, u.data_scope as dataScope
					from users u
					where u.username = :username and u.is_deleted = false
					""",
			nativeQuery = true)
	Optional<UserSecurityCredentialsProjection> findActiveCredentialsByUsername(@Param("username") String username);

	@Query(
			value = """
					select u.id as id, u.username as username, u.password_hash as passwordHash,
					       u.enabled as enabled, u.data_scope as dataScope
					from users u
					where u.id = :id and u.is_deleted = false
					""",
			nativeQuery = true)
	Optional<UserSecurityCredentialsProjection> findActiveCredentialsById(@Param("id") UUID id);

	@Query(
			value = """
					select distinct r.code
					from authz.user_roles ur
					inner join authz.roles r on r.id = ur.role_id
					where ur.user_id = :userId
					  and ur.is_deleted = false
					  and r.is_deleted = false
					  and r.enabled = true
					order by r.code
					""",
			nativeQuery = true)
	List<String> findActiveRoleCodesByUserId(@Param("userId") UUID userId);

	@Query(
			value = """
					select distinct p.code
					from authz.user_roles ur
					inner join authz.roles r on r.id = ur.role_id and r.is_deleted = false
					inner join authz.role_permissions rp on rp.role_id = r.id and rp.is_deleted = false
					inner join authz.permissions p on p.id = rp.permission_id and p.is_deleted = false
					where ur.user_id = :userId
					  and ur.is_deleted = false
					""",
			nativeQuery = true)
	List<String> findActivePermissionCodesViaRolesByUserId(@Param("userId") UUID userId);

	@Query(
			value = """
					select distinct p.code
					from authz.user_permissions up
					inner join authz.permissions p on p.id = up.permission_id and p.is_deleted = false and p.enabled = true
					where up.user_id = :userId
					  and up.is_deleted = false
					  and up.effect_type = 'GRANT'
					""",
			nativeQuery = true)
	List<String> findActiveDirectGrantedPermissionCodesByUserId(@Param("userId") UUID userId);

	@Query(
			value = """
					select distinct p.code
					from authz.user_permissions up
					inner join authz.permissions p on p.id = up.permission_id and p.is_deleted = false and p.enabled = true
					where up.user_id = :userId
					  and up.is_deleted = false
					  and up.effect_type = 'DENY'
					""",
			nativeQuery = true)
	List<String> findActiveDirectDeniedPermissionCodesByUserId(@Param("userId") UUID userId);
}
