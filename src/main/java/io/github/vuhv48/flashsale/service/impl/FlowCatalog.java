package io.github.vuhv48.flashsale.service.impl;

import io.github.vuhv48.flashsale.common.api.ErrorStatus;
import io.github.vuhv48.flashsale.common.exception.DomainException;
import io.github.vuhv48.flashsale.persistence.repository.FlowStepRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FlowCatalog {

	public static final String EVENT_LIFECYCLE = "EVENT_LIFECYCLE";
	public static final String ORDER_LIFECYCLE = "ORDER_LIFECYCLE";
	public static final String REFUND_PROCESS = "REFUND_PROCESS";

	private final FlowStepRepository flowStepRepository;
	private final Map<String, UUID> flowIds = new ConcurrentHashMap<>();
	private final Map<String, UUID> stepIds = new ConcurrentHashMap<>();

	public FlowCatalog(FlowStepRepository flowStepRepository) {
		this.flowStepRepository = flowStepRepository;
	}

	public UUID flowId(String flowCode) {
		ensureLoaded();
		UUID id = flowIds.get(flowCode);
		if (id == null) {
			throw new DomainException(ErrorStatus.ILLEGAL_STATE, "Thiếu quy trình " + flowCode);
		}
		return id;
	}

	public UUID stepId(String flowCode, String stepCode) {
		ensureLoaded();
		UUID id = stepIds.get(key(flowCode, stepCode));
		if (id == null) {
			throw new DomainException(ErrorStatus.ILLEGAL_STATE, "Thiếu bước " + flowCode + "/" + stepCode);
		}
		return id;
	}

	private void ensureLoaded() {
		if (!stepIds.isEmpty()) {
			return;
		}
		synchronized (this) {
			if (!stepIds.isEmpty()) {
				return;
			}
			for (Object[] row : flowStepRepository.allEnabledSteps()) {
				String flowCode = (String) row[0];
				UUID flowId = (UUID) row[1];
				String stepCode = (String) row[2];
				UUID stepId = (UUID) row[3];
				flowIds.put(flowCode, flowId);
				stepIds.put(key(flowCode, stepCode), stepId);
			}
		}
	}

	private static String key(String flowCode, String stepCode) {
		return flowCode + "|" + stepCode;
	}
}
