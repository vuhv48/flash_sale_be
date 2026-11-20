package io.github.vuhv48.flashsale.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.vuhv48.flashsale.common.api.ErrorStatus;
import io.github.vuhv48.flashsale.common.exception.DomainException;
import io.github.vuhv48.flashsale.event.EventImageView;
import io.github.vuhv48.flashsale.event.EventPage;
import io.github.vuhv48.flashsale.event.EventStatus;
import io.github.vuhv48.flashsale.event.OrganizerEventCard;
import io.github.vuhv48.flashsale.event.OrganizerEventDetail;
import io.github.vuhv48.flashsale.event.OrganizerTicketType;
import io.github.vuhv48.flashsale.event.PublicEventCard;
import io.github.vuhv48.flashsale.event.PublicEventDetail;
import io.github.vuhv48.flashsale.event.PublicTicketOffer;
import io.github.vuhv48.flashsale.event.TicketAvailability;
import io.github.vuhv48.flashsale.persistence.entity.DocumentEntity;
import io.github.vuhv48.flashsale.persistence.entity.EventDocumentEntity;
import io.github.vuhv48.flashsale.persistence.entity.EventEntity;
import io.github.vuhv48.flashsale.persistence.entity.ProvinceEntity;
import io.github.vuhv48.flashsale.persistence.entity.SystemConfigEntity;
import io.github.vuhv48.flashsale.persistence.entity.TicketTypeEntity;
import io.github.vuhv48.flashsale.persistence.repository.DocumentRepository;
import io.github.vuhv48.flashsale.persistence.repository.EventDocumentRepository;
import io.github.vuhv48.flashsale.persistence.repository.EventRepository;
import io.github.vuhv48.flashsale.persistence.repository.ProvinceRepository;
import io.github.vuhv48.flashsale.persistence.repository.SystemConfigRepository;
import io.github.vuhv48.flashsale.persistence.repository.TicketTypeRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EventQueryService {

	private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
	private static final String COVER = "EVENT_COVER";
	private static final String EVENT_CATEGORY = "EVENT_CATEGORY";

	private final EventRepository eventRepository;
	private final TicketTypeRepository ticketTypeRepository;
	private final EventDocumentRepository eventDocumentRepository;
	private final DocumentRepository documentRepository;
	private final ProvinceRepository provinceRepository;
	private final SystemConfigRepository systemConfigRepository;
	private final EventCache eventCache;
	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	public EventQueryService(
			EventRepository eventRepository,
			TicketTypeRepository ticketTypeRepository,
			EventDocumentRepository eventDocumentRepository,
			DocumentRepository documentRepository,
			ProvinceRepository provinceRepository,
			SystemConfigRepository systemConfigRepository,
			EventCache eventCache
	) {
		this.eventRepository = eventRepository;
		this.ticketTypeRepository = ticketTypeRepository;
		this.eventDocumentRepository = eventDocumentRepository;
		this.documentRepository = documentRepository;
		this.provinceRepository = provinceRepository;
		this.systemConfigRepository = systemConfigRepository;
		this.eventCache = eventCache;
	}

	@Transactional(readOnly = true)
	public EventPage<PublicEventCard> publicList(String category, UUID provinceId, LocalDate date, int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 50);
		String categoryCode = blank(category) ? null : category.trim().toUpperCase();
		String cacheKey = "flash-sale:events:list:" + eventCache.listVersion() + ":" + categoryCode + ":"
				+ provinceId + ":" + date + ":" + safePage + ":" + safeSize;
		String cached = eventCache.get(cacheKey);
		if (cached != null) {
			EventPage<PublicEventCard> pageResult = readPage(cached);
			if (pageResult != null) {
				return pageResult;
			}
		}
		Instant from = null;
		Instant to = null;
		if (date != null) {
			from = date.atStartOfDay(VIETNAM).toInstant();
			to = date.plusDays(1).atStartOfDay(VIETNAM).toInstant();
		}
		Page<EventEntity> result = eventRepository.findAll(publicSpec(categoryCode, provinceId, from, to),
				PageRequest.of(safePage, safeSize, Sort.by("saleStartsAt").ascending()));
		EventPage<PublicEventCard> body = new EventPage<>(cards(result.getContent()), result.getTotalElements(), safePage, safeSize);
		write(cacheKey, body);
		return body;
	}

	@Transactional(readOnly = true)
	public PublicEventDetail publicDetail(UUID eventId) {
		String cacheKey = EventCache.detailKey(eventId);
		String cached = eventCache.get(cacheKey);
		if (cached != null) {
			PublicEventDetail detail = readDetail(cached);
			if (detail != null) {
				return detail;
			}
		}
		EventEntity event = eventRepository.findByIdAndIsDeletedFalse(eventId)
				.orElseThrow(() -> new DomainException(ErrorStatus.EVENT_NOT_FOUND, ErrorStatus.EVENT_NOT_FOUND.defaultMessage()));
		if (!EventStatus.isPublic(event.getStatus())) {
			throw new DomainException(ErrorStatus.EVENT_NOT_FOUND, ErrorStatus.EVENT_NOT_FOUND.defaultMessage());
		}
		PublicEventDetail detail = toPublicDetail(event);
		write(cacheKey, detail);
		return detail;
	}

	@Transactional(readOnly = true)
	public EventPage<OrganizerEventCard> organizerList(UUID organizerId, int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 50);
		Page<EventEntity> result = eventRepository.findAll(
				(root, query, cb) -> cb.and(cb.isFalse(root.get("isDeleted")), cb.equal(root.get("organizerId"), organizerId)),
				PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending()));
		return new EventPage<>(organizerCards(result.getContent()), result.getTotalElements(), safePage, safeSize);
	}

	@Transactional(readOnly = true)
	public OrganizerEventDetail organizerDetail(UUID organizerId, UUID eventId) {
		EventEntity event = eventRepository.findByIdAndIsDeletedFalse(eventId)
				.orElseThrow(() -> new DomainException(ErrorStatus.EVENT_NOT_FOUND, ErrorStatus.EVENT_NOT_FOUND.defaultMessage()));
		if (!event.getOrganizerId().equals(organizerId)) {
			throw new DomainException(ErrorStatus.EVENT_NOT_FOUND, ErrorStatus.EVENT_NOT_FOUND.defaultMessage());
		}
		return organizerDetail(event);
	}

	@Transactional(readOnly = true)
	public EventPage<OrganizerEventCard> adminList(String status, int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 50);
		String statusCode = blank(status) ? null : status.trim().toUpperCase();
		if (statusCode != null && !known(statusCode)) {
			throw new DomainException(ErrorStatus.VALIDATION_ERROR, "Trạng thái không hợp lệ");
		}
		Page<EventEntity> result = eventRepository.findAll((root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.isFalse(root.get("isDeleted")));
			if (statusCode != null) {
				predicates.add(cb.equal(root.get("status"), statusCode));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		}, PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending()));
		return new EventPage<>(organizerCards(result.getContent()), result.getTotalElements(), safePage, safeSize);
	}

	@Transactional(readOnly = true)
	public OrganizerEventDetail adminDetail(UUID eventId) {
		EventEntity event = eventRepository.findByIdAndIsDeletedFalse(eventId)
				.orElseThrow(() -> new DomainException(ErrorStatus.EVENT_NOT_FOUND, ErrorStatus.EVENT_NOT_FOUND.defaultMessage()));
		return organizerDetail(event);
	}

	public OrganizerEventDetail organizerDetail(EventEntity event) {
		Map<String, String> categories = categoryLabels();
		String provinceName = provinceName(event.getProvinceId());
		List<OrganizerTicketType> tickets = ticketTypeRepository.findByEventIdAndIsDeletedFalseOrderBySortOrderAscNameAsc(event.getId())
				.stream()
				.map(ticket -> new OrganizerTicketType(
						ticket.getId(), ticket.getName(), ticket.getPrice(), ticket.getTotalQuantity(),
						ticket.getHeldQuantity(), ticket.getSoldQuantity(), ticket.available(),
						ticket.getSortOrder(), ticket.isSalePaused()))
				.toList();
		return new OrganizerEventDetail(
				event.getId(), event.getCode(), event.getName(), event.getDescription(), event.getCategory(),
				categories.get(event.getCategory()), event.getProvinceId(), provinceName, event.getVenueName(),
				event.getVenueAddress(), event.getStartsAt(), event.getEndsAt(), event.getWaitingRoomOpensAt(),
				event.getSaleStartsAt(), event.getSaleEndsAt(), event.getMaxTicketsPerUser(), event.getHoldMinutes(),
				event.getAdmissionRate(), event.getStatus(), EventStatus.label(event.getStatus()), event.getSubmittedAt(),
				event.getReviewedAt(), event.getRejectReason(), event.getPauseCount(), event.getPausedAt(),
				event.getCancelReason(), event.getOrganizerId(), tickets, images(event.getId()));
	}

	private PublicEventDetail toPublicDetail(EventEntity event) {
		Map<String, String> categories = categoryLabels();
		List<PublicTicketOffer> tickets = ticketTypeRepository.findByEventIdAndIsDeletedFalseOrderBySortOrderAscNameAsc(event.getId())
				.stream()
				.map(this::offer)
				.toList();
		Instant now = Instant.now();
		String target = "NONE";
		long seconds = 0;
		if (EventStatus.APPROVED.equals(event.getStatus()) && event.getSaleStartsAt() != null) {
			target = "SALE_START";
			seconds = Math.max(0, Duration.between(now, event.getSaleStartsAt()).getSeconds());
		} else if ((EventStatus.ON_SALE.equals(event.getStatus()) || EventStatus.PAUSED.equals(event.getStatus()))
				&& event.getSaleEndsAt() != null) {
			target = "SALE_END";
			seconds = Math.max(0, Duration.between(now, event.getSaleEndsAt()).getSeconds());
		}
		UUID coverId = images(event.getId()).stream()
				.filter(image -> COVER.equals(image.type()))
				.map(EventImageView::documentId)
				.findFirst()
				.orElse(null);
		return new PublicEventDetail(
				event.getId(), event.getCode(), event.getName(), event.getDescription(), event.getCategory(),
				categories.get(event.getCategory()), provinceName(event.getProvinceId()), event.getVenueName(),
				event.getVenueAddress(), event.getStartsAt(), event.getEndsAt(), event.getWaitingRoomOpensAt(),
				event.getSaleStartsAt(), event.getSaleEndsAt(), event.getStatus(), EventStatus.label(event.getStatus()),
				coverId, tickets, now, target, seconds);
	}

	private List<PublicEventCard> cards(List<EventEntity> events) {
		if (events.isEmpty()) {
			return List.of();
		}
		Map<String, String> categories = categoryLabels();
		Map<UUID, String> provinces = provinceNames(events);
		Map<UUID, Long> minPrices = new HashMap<>();
		Map<UUID, UUID> covers = new HashMap<>();
		List<UUID> ids = events.stream().map(EventEntity::getId).toList();
		for (TicketTypeEntity ticket : ticketTypeRepository.findByEventIdInAndIsDeletedFalse(ids)) {
			minPrices.merge(ticket.getEventId(), ticket.getPrice(), Math::min);
		}
		for (EventDocumentEntity image : eventDocumentRepository.findByEventIdInAndIsDeletedFalse(ids)) {
			if (COVER.equals(image.getDocumentTypeCode())) {
				covers.putIfAbsent(image.getEventId(), image.getDocumentId());
			}
		}
		return events.stream().map(event -> new PublicEventCard(
				event.getId(), event.getCode(), event.getName(), event.getCategory(), categories.get(event.getCategory()),
				provinces.get(event.getProvinceId()), event.getVenueName(), event.getStartsAt(), event.getSaleStartsAt(),
				event.getSaleEndsAt(), event.getStatus(), EventStatus.label(event.getStatus()),
				covers.get(event.getId()), minPrices.get(event.getId()))).toList();
	}

	private List<OrganizerEventCard> organizerCards(List<EventEntity> events) {
		if (events.isEmpty()) {
			return List.of();
		}
		Map<String, String> categories = categoryLabels();
		Map<UUID, String> provinces = provinceNames(events);
		Map<UUID, Integer> counts = new HashMap<>();
		List<UUID> ids = events.stream().map(EventEntity::getId).toList();
		for (TicketTypeEntity ticket : ticketTypeRepository.findByEventIdInAndIsDeletedFalse(ids)) {
			counts.merge(ticket.getEventId(), 1, Integer::sum);
		}
		return events.stream().map(event -> new OrganizerEventCard(
				event.getId(), event.getCode(), event.getName(), event.getCategory(), categories.get(event.getCategory()),
				provinces.get(event.getProvinceId()), event.getStatus(), EventStatus.label(event.getStatus()),
				event.getSaleStartsAt(), event.getStartsAt(), event.getSubmittedAt(), event.getRejectReason(),
				event.getPauseCount(), counts.getOrDefault(event.getId(), 0), event.getOrganizerId())).toList();
	}

	private List<EventImageView> images(UUID eventId) {
		List<EventDocumentEntity> links = eventDocumentRepository.findByEventIdAndIsDeletedFalseOrderBySortOrderAsc(eventId);
		if (links.isEmpty()) {
			return List.of();
		}
		Map<UUID, DocumentEntity> documents = new HashMap<>();
		for (DocumentEntity document : documentRepository.findAllById(links.stream().map(EventDocumentEntity::getDocumentId).toList())) {
			documents.put(document.getId(), document);
		}
		return links.stream().map(link -> {
			DocumentEntity document = documents.get(link.getDocumentId());
			return new EventImageView(link.getId(), link.getDocumentTypeCode(), link.getDocumentId(),
					document == null ? null : document.getOriginalFilename(),
					document == null ? null : document.getMimeType(),
					link.getSortOrder());
		}).toList();
	}

	private PublicTicketOffer offer(TicketTypeEntity ticket) {
		String code = TicketAvailability.code(ticket.getTotalQuantity(), ticket.available());
		return new PublicTicketOffer(ticket.getId(), ticket.getName(), ticket.getPrice(), code, TicketAvailability.label(code), ticket.getSortOrder());
	}

	private Map<String, String> categoryLabels() {
		Map<String, String> labels = new HashMap<>();
		for (SystemConfigEntity config : systemConfigRepository.findByGroupCodeAndActiveTrueAndIsDeletedFalseOrderByOrderNoAsc(EVENT_CATEGORY)) {
			labels.put(config.getCode(), config.getLabel());
		}
		return labels;
	}

	private String provinceName(UUID provinceId) {
		if (provinceId == null) {
			return null;
		}
		return provinceRepository.findById(provinceId).map(ProvinceEntity::getName).orElse(null);
	}

	private Map<UUID, String> provinceNames(List<EventEntity> events) {
		List<UUID> ids = events.stream().map(EventEntity::getProvinceId).filter(id -> id != null).distinct().toList();
		Map<UUID, String> names = new HashMap<>();
		if (!ids.isEmpty()) {
			for (ProvinceEntity province : provinceRepository.findAllById(ids)) {
				names.put(province.getId(), province.getName());
			}
		}
		return names;
	}

	private Specification<EventEntity> publicSpec(String category, UUID provinceId, Instant from, Instant to) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.isFalse(root.get("isDeleted")));
			predicates.add(root.get("status").in(EventStatus.PUBLIC));
			if (category != null) {
				predicates.add(cb.equal(root.get("category"), category));
			}
			if (provinceId != null) {
				predicates.add(cb.equal(root.get("provinceId"), provinceId));
			}
			if (from != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("startsAt"), from));
				predicates.add(cb.lessThan(root.get("startsAt"), to));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	private void write(String key, Object body) {
		try {
			eventCache.put(key, objectMapper.writeValueAsString(body));
		} catch (JsonProcessingException ex) {
			// Danh sách vẫn trả từ database khi không ghi được cache.
		}
	}

	private EventPage<PublicEventCard> readPage(String json) {
		try {
			return objectMapper.readValue(json, new TypeReference<EventPage<PublicEventCard>>() {
			});
		} catch (JsonProcessingException ex) {
			return null;
		}
	}

	private PublicEventDetail readDetail(String json) {
		try {
			return objectMapper.readValue(json, PublicEventDetail.class);
		} catch (JsonProcessingException ex) {
			return null;
		}
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}

	private static boolean known(String status) {
		return EventStatus.DRAFT.equals(status) || EventStatus.PENDING_REVIEW.equals(status)
				|| EventStatus.REJECTED.equals(status) || EventStatus.APPROVED.equals(status)
				|| EventStatus.ON_SALE.equals(status) || EventStatus.PAUSED.equals(status)
				|| EventStatus.SALE_ENDED.equals(status) || EventStatus.CANCELLED.equals(status)
				|| EventStatus.COMPLETED.equals(status);
	}
}
