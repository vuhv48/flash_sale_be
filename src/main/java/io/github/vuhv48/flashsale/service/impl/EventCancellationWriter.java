package io.github.vuhv48.flashsale.service.impl;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class EventCancellationWriter {

	private final EntityManager entityManager;

	public EventCancellationWriter(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public record OrderRow(UUID id, UUID userId, String orderCode, long totalAmount, UUID currentStepId, String status, String email) {
	}

	public int releaseHolds(UUID eventId, String actorName) {
		return entityManager.createNativeQuery("""
				UPDATE reservations
				SET status = 'CANCELLED',
				    released_at = now(),
				    release_reason = 'EVENT_CANCELLED',
				    updated_at = now(),
				    updated_by = :actor
				WHERE event_id = :eventId AND status = 'HELD' AND is_deleted = false
				""")
				.setParameter("eventId", eventId)
				.setParameter("actor", actorName)
				.executeUpdate();
	}

	@SuppressWarnings("unchecked")
	public List<OrderRow> activeOrders(UUID eventId) {
		List<Object[]> rows = entityManager.createNativeQuery("""
				SELECT o.id, o.user_id, o.order_code, o.total_amount, o.current_step_id, o.status, u.email
				FROM orders o
				JOIN users u ON u.id = o.user_id
				WHERE o.event_id = :eventId
				  AND o.is_deleted = false
				  AND o.status IN ('PENDING_PAYMENT', 'PAID', 'ISSUED')
				""")
				.setParameter("eventId", eventId)
				.getResultList();
		List<OrderRow> orders = new ArrayList<>();
		for (Object[] row : rows) {
			orders.add(new OrderRow(
					uuid(row[0]),
					uuid(row[1]),
					(String) row[2],
					((Number) row[3]).longValue(),
					uuid(row[4]),
					(String) row[5],
					(String) row[6]
			));
		}
		return orders;
	}

	public void moveOrder(OrderRow order, String nextStatus, UUID nextStepId, String action, UUID actorId, String actorName, String reason) {
		String storedReason = reason == null ? null : reason.substring(0, Math.min(reason.length(), 300));
		entityManager.createNativeQuery("""
				INSERT INTO order_flows (order_id, step_no, from_step_id, flow_step_id, action, processed_by, processed_by_name, reason)
				SELECT :orderId, n.next_no,
				       CASE WHEN n.next_no = 1 THEN NULL ELSE :fromStep END,
				       :toStep, :action, :actorId, :actorName, :reason
				FROM (SELECT COALESCE(MAX(step_no), 0) + 1 AS next_no FROM order_flows WHERE order_id = :orderId) n
				""")
				.setParameter("orderId", order.id())
				.setParameter("fromStep", order.currentStepId())
				.setParameter("toStep", nextStepId)
				.setParameter("action", action)
				.setParameter("actorId", actorId)
				.setParameter("actorName", actorName)
				.setParameter("reason", storedReason)
				.executeUpdate();
		entityManager.createNativeQuery("""
				UPDATE orders
				SET status = :status,
				    current_step_id = :stepId,
				    cancelled_at = CASE WHEN :status = 'CANCELLED' THEN now() ELSE cancelled_at END,
				    cancel_reason = CASE WHEN :status = 'CANCELLED' THEN :reason ELSE cancel_reason END,
				    updated_at = now(),
				    updated_by = :actorName
				WHERE id = :orderId
				""")
				.setParameter("status", nextStatus)
				.setParameter("stepId", nextStepId)
				.setParameter("reason", storedReason)
				.setParameter("actorName", actorName)
				.setParameter("orderId", order.id())
				.executeUpdate();
	}

	public void insertRefund(OrderRow order, UUID flowId, UUID requestedStepId, UUID actorId, String actorName, String reason) {
		String storedReason = reason == null ? null : reason.substring(0, Math.min(reason.length(), 500));
		Object refundId = entityManager.createNativeQuery("""
				INSERT INTO refunds (order_id, amount, reason_type, reason, status, flow_id, current_step_id, requested_by, created_by, updated_by)
				VALUES (:orderId, :amount, 'EVENT_CANCELLED', :reason, 'REQUESTED', :flowId, :stepId, :actorId, :actorName, :actorName)
				RETURNING id
				""")
				.setParameter("orderId", order.id())
				.setParameter("amount", order.totalAmount())
				.setParameter("reason", storedReason)
				.setParameter("flowId", flowId)
				.setParameter("stepId", requestedStepId)
				.setParameter("actorId", actorId)
				.setParameter("actorName", actorName)
				.getSingleResult();
		entityManager.createNativeQuery("""
				INSERT INTO refund_flows (refund_id, step_no, from_step_id, flow_step_id, action, processed_by, processed_by_name, reason)
				VALUES (:refundId, 1, NULL, :stepId, 'REQUEST', :actorId, :actorName, :reason)
				""")
				.setParameter("refundId", uuid(refundId))
				.setParameter("stepId", requestedStepId)
				.setParameter("actorId", actorId)
				.setParameter("actorName", actorName)
				.setParameter("reason", storedReason)
				.executeUpdate();
	}

	public int revokeTickets(UUID eventId, String actorName, String reason) {
		String storedReason = reason == null ? null : reason.substring(0, Math.min(reason.length(), 300));
		return entityManager.createNativeQuery("""
				UPDATE tickets
				SET status = 'REVOKED',
				    revoked_at = now(),
				    revoke_reason = :reason,
				    updated_at = now(),
				    updated_by = :actor
				WHERE event_id = :eventId AND status = 'VALID' AND is_deleted = false
				""")
				.setParameter("eventId", eventId)
				.setParameter("reason", storedReason)
				.setParameter("actor", actorName)
				.executeUpdate();
	}

	private static UUID uuid(Object value) {
		if (value instanceof UUID id) {
			return id;
		}
		return UUID.fromString(value.toString());
	}
}
