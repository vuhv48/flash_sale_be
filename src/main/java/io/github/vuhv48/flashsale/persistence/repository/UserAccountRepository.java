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

	@Query("select case when count(u) > 0 then true else false end from UserAccount u where lower(u.email) = :email and u.isDeleted = false")
	boolean existsByEmail(@Param("email") String email);

	@Query("select case when count(u) > 0 then true else false end from UserAccount u where u.phone = :phone and u.isDeleted = false and u.id <> :userId")
	boolean existsByPhoneAndIdNot(@Param("phone") String phone, @Param("userId") UUID userId);

	@Query("select case when count(u) > 0 then true else false end from UserAccount u where u.phone = :phone and u.isDeleted = false")
	boolean existsByPhone(@Param("phone") String phone);

	@Query("select u from UserAccount u where lower(u.email) = :email and u.isDeleted = false")
	Optional<UserAccount> findByEmail(@Param("email") String email);

	@Query("""
			select count(distinct u.id) from UserAccount u join u.roles r
			where r.code = 'ADMIN' and u.isDeleted = false and r.isDeleted = false and r.enabled = true
			""")
	long countActiveAdmins();

	@Query("select u from UserAccount u where u.username = :username and u.isDeleted = false")
	Optional<UserAccount> findByUsername(@Param("username") String username);

	@Query("select u from UserAccount u where u.id = :id and u.isDeleted = false")
	Optional<UserAccount> findActiveById(@Param("id") UUID id);

	@Query("""
			select u from UserAccount u
			where u.isDeleted = false
			  and (
			    lower(u.username) like lower(concat('%', :term, '%'))
			    or lower(coalesce(u.email, '')) like lower(concat('%', :term, '%'))
			    or lower(coalesce(u.fullName, '')) like lower(concat('%', :term, '%'))
			  )
			order by u.username
			""")
	List<UserAccount> searchActive(@Param("term") String term, org.springframework.data.domain.Pageable pageable);

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
					where lower(u.email) = :email and u.is_deleted = false
					""",
			nativeQuery = true)
	Optional<UserSecurityCredentialsProjection> findActiveCredentialsByEmail(@Param("email") String email);

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

	@Query(
			value = """
					select p.code as "code",
					       p.name as "name",
					       p.permission_group as "permissionGroup",
					       p.action_code as "actionCode",
					       rp.access_level as "accessLevel"
					from authz.user_roles ur
					inner join authz.roles r on r.id = ur.role_id and r.is_deleted = false and r.enabled = true
					inner join authz.role_permissions rp on rp.role_id = r.id and rp.is_deleted = false
					inner join authz.permissions p on p.id = rp.permission_id and p.is_deleted = false and p.enabled = true
					where ur.user_id = :userId
					  and ur.is_deleted = false
					""",
			nativeQuery = true)
	List<EffectivePermissionRow> findRolePermissionRowsByUserId(@Param("userId") UUID userId);

	@Query(
			value = """
					select p.code as "code",
					       p.name as "name",
					       p.permission_group as "permissionGroup",
					       p.action_code as "actionCode",
					       null as "accessLevel"
					from authz.user_permissions up
					inner join authz.permissions p on p.id = up.permission_id and p.is_deleted = false and p.enabled = true
					where up.user_id = :userId
					  and up.is_deleted = false
					  and up.effect_type = 'GRANT'
					""",
			nativeQuery = true)
	List<EffectivePermissionRow> findDirectGrantedPermissionRowsByUserId(@Param("userId") UUID userId);
}
