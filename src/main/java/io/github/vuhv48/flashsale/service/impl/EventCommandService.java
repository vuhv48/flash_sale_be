package io.github.vuhv48.flashsale.service.impl;

import io.github.vuhv48.flashsale.common.api.ErrorStatus;
import io.github.vuhv48.flashsale.common.exception.DomainException;
import io.github.vuhv48.flashsale.event.AttachEventImageRequest;
import io.github.vuhv48.flashsale.event.EventStatus;
import io.github.vuhv48.flashsale.event.OrganizerEventDetail;
import io.github.vuhv48.flashsale.event.SaveEventRequest;
import io.github.vuhv48.flashsale.event.SaveTicketTypeRequest;
import io.github.vuhv48.flashsale.persistence.entity.DocumentEntity;
import io.github.vuhv48.flashsale.persistence.entity.DocumentTypeEntity;
import io.github.vuhv48.flashsale.persistence.entity.EventDocumentEntity;
import io.github.vuhv48.flashsale.persistence.entity.EventEntity;
import io.github.vuhv48.flashsale.persistence.entity.TicketTypeEntity;
import io.github.vuhv48.flashsale.persistence.repository.DocumentRepository;
import io.github.vuhv48.flashsale.persistence.repository.DocumentTypeRepository;
import io.github.vuhv48.flashsale.persistence.repository.EventDocumentRepository;
import io.github.vuhv48.flashsale.persistence.repository.EventRepository;
import io.github.vuhv48.flashsale.persistence.repository.ProvinceRepository;
import io.github.vuhv48.flashsale.persistence.repository.SystemConfigRepository;
import io.github.vuhv48.flashsale.persistence.repository.TicketTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EventCommandService {

	private static final String EVENT_CATEGORY = "EVENT_CATEGORY";
	private static final String COVER = "EVENT_COVER";
	private static final String IMAGE = "EVENT_IMAGE";

	private final EventRepository eventRepository;
	private final TicketTypeRepository ticketTypeRepository;
	private final EventDocumentRepository eventDocumentRepository;
	private final DocumentRepository documentRepository;
	private final DocumentTypeRepository documentTypeRepository;
	private final ProvinceRepository provinceRepository;
	private final SystemConfigRepository systemConfigRepository;
	private final FlowCatalog flowCatalog;
	private final EventFlowRecorder flowRecorder;
	private final EventCache eventCache;
	private final EventQueryService eventQueryService;
	private final EventNotifier eventNotifier;

	public EventCommandService(
			EventRepository eventRepository,
			TicketTypeRepository ticketTypeRepository,
			EventDocumentRepository eventDocumentRepository,
			DocumentRepository documentRepository,
			DocumentTypeRepository documentTypeRepository,
			ProvinceRepository provinceRepository,
			SystemConfigRepository systemConfigRepository,
			FlowCatalog flowCatalog,
			EventFlowRecorder flowRecorder,
			EventCache eventCache,
			EventQueryService eventQueryService,
			EventNotifier eventNotifier
	) {
		this.eventRepository = eventRepository;
		this.ticketTypeRepository = ticketTypeRepository;
		this.eventDocumentRepository = eventDocumentRepository;
		this.documentRepository = documentRepository;
		this.documentTypeRepository = documentTypeRepository;
		this.provinceRepository = provinceRepository;
		this.systemConfigRepository = systemConfigRepository;
		this.flowCatalog = flowCatalog;
		this.flowRecorder = flowRecorder;
		this.eventCache = eventCache;
		this.eventQueryService = eventQueryService;
		this.eventNotifier = eventNotifier;
	}

	@Transactional
	public OrganizerEventDetail create(UUID organizerId, String actorName, SaveEventRequest request) {
		EventEntity event = new EventEntity();
		event.setCode(eventRepository.nextCode());
		event.setOrganizerId(organizerId);
		event.setName(request.name());
		event.setMaxTicketsPerUser(request.maxTicketsPerUser() == null ? 4 : request.maxTicketsPerUser());
		event.setHoldMinutes(10);
		event.setAdmissionRate(200);
		event.setPauseCount(0);
		event.setStatus(EventStatus.DRAFT);
		event.setFlowId(flowCatalog.flowId(FlowCatalog.EVENT_LIFECYCLE));
		event.setCurrentStepId(flowCatalog.stepId(FlowCatalog.EVENT_LIFECYCLE, EventStatus.DRAFT));
		event.setCreatedBy(actorName);
		event.setUpdatedBy(actorName);
		apply(event, request);
		checkReferences(event);
		checkTimeOrder(event);
		eventRepository.save(event);
		flowRecorder.recordFirst(event, organizerId, actorName);
		return eventQueryService.organizerDetail(event);
	}

	@Transactional
	public OrganizerEventDetail update(UUID organizerId, String actorName, UUID eventId, SaveEventRequest request) {
		EventEntity event = editable(eventId, organizerId);
		apply(event, request);
		checkReferences(event);
		checkTimeOrder(event);
		reopenIfRejected(event, organizerId, actorName);
		event.setUpdatedBy(actorName);
		eventCache.evict(event.getId());
		return eventQueryService.organizerDetail(event);
	}

	@Transactional
	public OrganizerEventDetail addTicket(UUID organizerId, String actorName, UUID eventId, SaveTicketTypeRequest request) {
		EventEntity event = editable(eventId, organizerId);
		if (ticketTypeRepository.countByEventIdAndIsDeletedFalse(eventId) >= 10) {
			throw new DomainException(ErrorStatus.TICKET_TYPE_LIMIT, ErrorStatus.TICKET_TYPE_LIMIT.defaultMessage());
		}
		ensureUniqueName(eventId, request.name(), null);
		TicketTypeEntity ticket = new TicketTypeEntity();
		ticket.setEventId(eventId);
		ticket.setHeldQuantity(0);
		ticket.setSoldQuantity(0);
		ticket.setSalePaused(false);
		ticket.setCreatedBy(actorName);
		fillTicket(ticket, request, actorName);
		ticketTypeRepository.save(ticket);
		reopenIfRejected(event, organizerId, actorName);
		eventCache.evict(eventId);
		return eventQueryService.organizerDetail(event);
	}

	@Transactional
	public OrganizerEventDetail updateTicket(UUID organizerId, String actorName, UUID eventId, UUID ticketTypeId, SaveTicketTypeRequest request) {
		EventEntity event = editable(eventId, organizerId);
		TicketTypeEntity ticket = ticketTypeRepository.findByIdAndEventIdAndIsDeletedFalse(ticketTypeId, eventId)
				.orElseThrow(() -> new DomainException(ErrorStatus.TICKET_TYPE_NOT_FOUND, ErrorStatus.TICKET_TYPE_NOT_FOUND.defaultMessage()));
		if (request.totalQuantity() < ticket.getHeldQuantity() + ticket.getSoldQuantity()) {
			throw new DomainException(ErrorStatus.VALIDATION_ERROR, "Tổng số lượng không được nhỏ hơn số vé đang giữ và đã bán");
		}
		ensureUniqueName(eventId, request.name(), ticket.getId());
		fillTicket(ticket, request, actorName);
		reopenIfRejected(event, organizerId, actorName);
		eventCache.evict(eventId);
		return eventQueryService.organizerDetail(event);
	}

	@Transactional
	public OrganizerEventDetail deleteTicket(UUID organizerId, String actorName, UUID eventId, UUID ticketTypeId) {
		EventEntity event = editable(eventId, organizerId);
		TicketTypeEntity ticket = ticketTypeRepository.findByIdAndEventIdAndIsDeletedFalse(ticketTypeId, eventId)
				.orElseThrow(() -> new DomainException(ErrorStatus.TICKET_TYPE_NOT_FOUND, ErrorStatus.TICKET_TYPE_NOT_FOUND.defaultMessage()));
		if (ticket.getHeldQuantity() + ticket.getSoldQuantity() > 0) {
			throw new DomainException(ErrorStatus.EVENT_STATUS_INVALID, "Không xóa loại vé đã có vé đang giữ hoặc đã bán");
		}
		ticket.setDeleted(true);
		ticket.setUpdatedBy(actorName);
		reopenIfRejected(event, organizerId, actorName);
		eventCache.evict(eventId);
		return eventQueryService.organizerDetail(event);
	}

	@Transactional
	public OrganizerEventDetail attachImage(UUID organizerId, String actorName, UUID eventId, AttachEventImageRequest request) {
		EventEntity event = editable(eventId, organizerId);
		if (!COVER.equals(request.type()) && !IMAGE.equals(request.type())) {
			throw new DomainException(ErrorStatus.VALIDATION_ERROR, "Loại ảnh phải là EVENT_COVER hoặc EVENT_IMAGE");
		}
		DocumentTypeEntity type = documentTypeRepository.findByCodeAndIsDeletedFalse(request.type())
				.orElseThrow(() -> new DomainException(ErrorStatus.VALIDATION_ERROR, "Loại ảnh không có trong danh mục"));
		if (type.getAllowedMimePattern() != null && !request.mimeType().matches(type.getAllowedMimePattern())) {
			throw new DomainException(ErrorStatus.VALIDATION_ERROR, "Ảnh chỉ nhận jpeg, png hoặc webp");
		}
		if (type.getMaxSizeMb() != null && request.sizeBytes() > type.getMaxSizeMb() * 1024L * 1024L) {
			throw new DomainException(ErrorStatus.VALIDATION_ERROR, "Ảnh vượt quá " + type.getMaxSizeMb() + " MB");
		}
		if (IMAGE.equals(request.type())) {
			int max = type.getMaxFilesPerRef() == null ? 10 : type.getMaxFilesPerRef();
			long current = eventDocumentRepository.countByEventIdAndDocumentTypeCodeAndIsDeletedFalse(eventId, IMAGE);
			if (current >= max) {
				throw new DomainException(ErrorStatus.VALIDATION_ERROR, "Mỗi sự kiện tối đa " + max + " ảnh phụ");
			}
		}
		if (COVER.equals(request.type())) {
			for (EventDocumentEntity old : eventDocumentRepository.findActive(eventId, COVER)) {
				old.setDeleted(true);
				old.setUpdatedBy(actorName);
			}
			eventDocumentRepository.flush();
		}
		String safeName = safeFileName(request.fileName());
		DocumentEntity document = new DocumentEntity();
		document.setBucket("flash-sale");
		document.setFilePath("events/" + eventId + "/" + UUID.randomUUID() + "/" + safeName);
		document.setOriginalFilename(safeName);
		document.setMimeType(request.mimeType());
		document.setSizeBytes(request.sizeBytes());
		document.setStatus("ATTACHED");
		document.setDocumentTypeCode(type.getCode());
		document.setOwnerId(organizerId);
		document.setVisibility(type.getDefaultVisibility());
		document.setAttachedAt(Instant.now());
		document.setCreatedBy(actorName);
		document.setUpdatedBy(actorName);
		documentRepository.save(document);

		EventDocumentEntity link = new EventDocumentEntity();
		link.setEventId(eventId);
		link.setDocumentTypeCode(type.getCode());
		link.setDocumentId(document.getId());
		link.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
		link.setCreatedBy(actorName);
		link.setUpdatedBy(actorName);
		eventDocumentRepository.save(link);
		reopenIfRejected(event, organizerId, actorName);
		eventCache.evict(eventId);
		return eventQueryService.organizerDetail(event);
	}

	@Transactional
	public OrganizerEventDetail deleteImage(UUID organizerId, String actorName, UUID eventId, UUID imageId) {
		EventEntity event = editable(eventId, organizerId);
		EventDocumentEntity image = eventDocumentRepository.findByIdAndEventIdAndIsDeletedFalse(imageId, eventId)
				.orElseThrow(() -> new DomainException(ErrorStatus.IMAGE_NOT_FOUND, ErrorStatus.IMAGE_NOT_FOUND.defaultMessage()));
		image.setDeleted(true);
		image.setUpdatedBy(actorName);
		reopenIfRejected(event, organizerId, actorName);
		eventCache.evict(eventId);
		return eventQueryService.organizerDetail(event);
	}

	@Transactional
	public OrganizerEventDetail submit(UUID organizerId, String actorName, UUID eventId) {
		EventEntity event = owned(eventId, organizerId);
		if (!EventStatus.DRAFT.equals(event.getStatus())) {
			throw new DomainException(ErrorStatus.EVENT_STATUS_INVALID, "Chỉ gửi duyệt được sự kiện đang nháp. Sự kiện bị từ chối cần sửa trước.");
		}
		List<String> errors = submitErrors(event, Instant.now());
		if (!errors.isEmpty()) {
			throw new DomainException(ErrorStatus.VALIDATION_ERROR, String.join("; ", errors));
		}
		event.setSubmittedAt(Instant.now());
		flowRecorder.move(event, EventStatus.PENDING_REVIEW, "SUBMIT", organizerId, actorName, null);
		eventNotifier.submitted(event, actorName);
		eventCache.evict(eventId);
		return eventQueryService.organizerDetail(event);
	}

	private EventEntity editable(UUID eventId, UUID organizerId) {
		EventEntity event = owned(eventId, organizerId);
		if (!EventStatus.isEditable(event.getStatus())) {
			throw new DomainException(ErrorStatus.EVENT_NOT_EDITABLE, "Chỉ sửa được sự kiện khi đang nháp hoặc bị từ chối. Sau khi duyệt không đổi giá, số lượng, giới hạn vé hoặc giờ mở bán.");
		}
		return event;
	}

	private EventEntity owned(UUID eventId, UUID organizerId) {
		EventEntity event = eventRepository.findByIdAndIsDeletedFalse(eventId)
				.orElseThrow(() -> new DomainException(ErrorStatus.EVENT_NOT_FOUND, ErrorStatus.EVENT_NOT_FOUND.defaultMessage()));
		if (!event.getOrganizerId().equals(organizerId)) {
			throw new DomainException(ErrorStatus.EVENT_NOT_FOUND, ErrorStatus.EVENT_NOT_FOUND.defaultMessage());
		}
		return event;
	}

	private void reopenIfRejected(EventEntity event, UUID actorId, String actorName) {
		if (EventStatus.REJECTED.equals(event.getStatus())) {
			flowRecorder.move(event, EventStatus.DRAFT, "EDIT", actorId, actorName, null);
		}
	}

	private void apply(EventEntity event, SaveEventRequest request) {
		event.setName(request.name());
		event.setDescription(request.description());
		event.setCategory(request.category());
		event.setVenueName(request.venueName());
		event.setVenueAddress(request.venueAddress());
		event.setProvinceId(request.provinceId());
		event.setStartsAt(request.startsAt());
		event.setEndsAt(request.endsAt());
		event.setWaitingRoomOpensAt(request.waitingRoomOpensAt());
		event.setSaleStartsAt(request.saleStartsAt());
		event.setSaleEndsAt(request.saleEndsAt());
		if (request.maxTicketsPerUser() != null) {
			event.setMaxTicketsPerUser(request.maxTicketsPerUser());
		}
	}

	private void checkReferences(EventEntity event) {
		if (event.getProvinceId() != null && provinceRepository.findByIdAndIsDeletedFalse(event.getProvinceId()).isEmpty()) {
			throw new DomainException(ErrorStatus.PROVINCE_NOT_FOUND, ErrorStatus.PROVINCE_NOT_FOUND.defaultMessage());
		}
		if (event.getCategory() != null
				&& systemConfigRepository.findByGroupCodeAndCodeAndActiveTrueAndIsDeletedFalse(EVENT_CATEGORY, event.getCategory()).isEmpty()) {
			throw new DomainException(ErrorStatus.CATEGORY_INVALID, ErrorStatus.CATEGORY_INVALID.defaultMessage());
		}
	}

	private void checkTimeOrder(EventEntity event) {
		List<String> errors = timeOrderErrors(event);
		if (!errors.isEmpty()) {
			throw new DomainException(ErrorStatus.VALIDATION_ERROR, String.join("; ", errors));
		}
	}

	private void ensureUniqueName(UUID eventId, String name, UUID excludeId) {
		if (ticketTypeRepository.existsName(eventId, name, excludeId)) {
			throw new DomainException(ErrorStatus.TICKET_TYPE_DUPLICATE, ErrorStatus.TICKET_TYPE_DUPLICATE.defaultMessage());
		}
	}

	private static void fillTicket(TicketTypeEntity ticket, SaveTicketTypeRequest request, String actorName) {
		if (request.price() % 1000 != 0) {
			throw new DomainException(ErrorStatus.VALIDATION_ERROR, "Giá phải là bội số của 1.000");
		}
		ticket.setName(request.name());
		ticket.setPrice(request.price());
		ticket.setTotalQuantity(request.totalQuantity());
		ticket.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
		ticket.setUpdatedBy(actorName);
	}

	private List<String> submitErrors(EventEntity event, Instant now) {
		List<String> errors = new ArrayList<>();
		if (event.getDescription() == null || event.getDescription().length() < 20) {
			errors.add("Mô tả phải từ 20 đến 10.000 ký tự");
		}
		if (event.getCategory() == null) {
			errors.add("Cần chọn thể loại");
		}
		if (event.getVenueName() == null || event.getVenueAddress() == null) {
			errors.add("Cần tên địa điểm và địa chỉ");
		}
		if (event.getProvinceId() == null) {
			errors.add("Cần chọn tỉnh, thành phố");
		}
		if (event.getWaitingRoomOpensAt() == null || event.getSaleStartsAt() == null || event.getSaleEndsAt() == null
				|| event.getStartsAt() == null || event.getEndsAt() == null) {
			errors.add("Cần đủ giờ mở phòng chờ, mở bán, đóng bán, bắt đầu và kết thúc");
		} else {
			errors.addAll(timeOrderErrors(event));
			Duration waitingGap = Duration.between(event.getWaitingRoomOpensAt(), event.getSaleStartsAt());
			if (waitingGap.compareTo(Duration.ofMinutes(5)) < 0 || waitingGap.compareTo(Duration.ofMinutes(60)) > 0) {
				errors.add("Giờ mở phòng chờ phải trước giờ mở bán từ 5 đến 60 phút");
			}
			if (event.getSaleStartsAt().isBefore(now.plus(Duration.ofHours(24)))) {
				errors.add("Giờ mở bán phải sau thời điểm gửi duyệt ít nhất 24 giờ");
			}
		}
		long tickets = ticketTypeRepository.countByEventIdAndIsDeletedFalse(event.getId());
		if (tickets < 1 || tickets > 10) {
			errors.add("Mỗi sự kiện có từ 1 đến 10 loại vé");
		}
		if (eventDocumentRepository.countByEventIdAndDocumentTypeCodeAndIsDeletedFalse(event.getId(), COVER) != 1) {
			errors.add("Sự kiện cần 1 ảnh bìa");
		}
		return errors;
	}

	private static List<String> timeOrderErrors(EventEntity event) {
		List<String> errors = new ArrayList<>();
		if (event.getWaitingRoomOpensAt() != null && event.getSaleStartsAt() != null
				&& !event.getWaitingRoomOpensAt().isBefore(event.getSaleStartsAt())) {
			errors.add("Giờ mở phòng chờ phải trước giờ mở bán");
		}
		if (event.getSaleStartsAt() != null && event.getSaleEndsAt() != null
				&& !event.getSaleStartsAt().isBefore(event.getSaleEndsAt())) {
			errors.add("Giờ đóng bán phải sau giờ mở bán");
		}
		if (event.getSaleEndsAt() != null && event.getStartsAt() != null
				&& event.getSaleEndsAt().isAfter(event.getStartsAt())) {
			errors.add("Giờ đóng bán phải trước hoặc bằng giờ sự kiện bắt đầu");
		}
		if (event.getStartsAt() != null && event.getEndsAt() != null
				&& !event.getStartsAt().isBefore(event.getEndsAt())) {
			errors.add("Giờ kết thúc phải sau giờ bắt đầu");
		}
		return errors;
	}

	private static String safeFileName(String fileName) {
		String safe = fileName.replace("\\", "").replace("/", "").replace("..", "");
		if (safe.isBlank()) {
			return "image";
		}
		return safe.length() <= 180 ? safe : safe.substring(0, 180);
	}
}
