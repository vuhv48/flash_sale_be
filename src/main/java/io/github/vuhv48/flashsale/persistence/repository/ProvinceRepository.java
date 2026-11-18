package io.github.vuhv48.flashsale.persistence.repository;

import io.github.vuhv48.flashsale.persistence.entity.ProvinceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProvinceRepository extends JpaRepository<ProvinceEntity, UUID> {

	List<ProvinceEntity> findByIsDeletedFalseOrderByOrderNoAsc();

	Optional<ProvinceEntity> findByIdAndIsDeletedFalse(UUID id);
}
