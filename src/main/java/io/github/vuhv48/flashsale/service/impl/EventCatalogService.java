package io.github.vuhv48.flashsale.service.impl;

import io.github.vuhv48.flashsale.event.CategoryItem;
import io.github.vuhv48.flashsale.event.ProvinceItem;
import io.github.vuhv48.flashsale.persistence.repository.ProvinceRepository;
import io.github.vuhv48.flashsale.persistence.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventCatalogService {

	private final ProvinceRepository provinceRepository;
	private final SystemConfigRepository systemConfigRepository;

	public EventCatalogService(ProvinceRepository provinceRepository, SystemConfigRepository systemConfigRepository) {
		this.provinceRepository = provinceRepository;
		this.systemConfigRepository = systemConfigRepository;
	}

	@Transactional(readOnly = true)
	public List<ProvinceItem> provinces() {
		return provinceRepository.findByIsDeletedFalseOrderByOrderNoAsc().stream()
				.map(province -> new ProvinceItem(province.getId(), province.getGsoCode(), province.getName(), province.getShortName(), province.getType()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<CategoryItem> categories() {
		return systemConfigRepository.findByGroupCodeAndActiveTrueAndIsDeletedFalseOrderByOrderNoAsc("EVENT_CATEGORY").stream()
				.map(config -> new CategoryItem(config.getCode(), config.getLabel()))
				.toList();
	}
}
