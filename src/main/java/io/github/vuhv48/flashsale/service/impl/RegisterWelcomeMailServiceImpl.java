package io.github.vuhv48.flashsale.service.impl;

import io.github.vuhv48.flashsale.service.RegisterWelcomeMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterWelcomeMailServiceImpl implements RegisterWelcomeMailService {

	private final ObjectProvider<JavaMailSender> mailSender;

	@Value("${app.mail.from:}")
	private String from;

	@Value("${app.mail.admin-notification-address:}")
	private String adminNotificationAddress;

	@Override
	public void enqueueWelcomeEmail(UUID userId, String username, String contactEmail) {
		String to = resolveToAddress(contactEmail, username);
		if (to == null) {
			log.debug("[mail] bỏ qua welcome — không có địa chỉ hợp lệ (username={})", username);
			return;
		}
		send(to, "Chào mừng tới Flash Sale", "Xin chào " + username + ", tài khoản của bạn đã được tạo.");
	}

	@Override
	public void enqueueAdminNewUserNotification(UUID newUserId, String newUsername, String contactEmail) {
		String adminTo = StringUtils.hasText(adminNotificationAddress) ? adminNotificationAddress.trim() : null;
		if (adminTo == null || !adminTo.contains("@")) {
			log.debug("[mail] bỏ qua mail admin — chưa cấu hình app.mail.admin-notification-address");
			return;
		}
		String contact = StringUtils.hasText(contactEmail) ? contactEmail.trim() : "(không có)";
		send(adminTo, "User mới: " + newUsername, "User " + newUsername + " (" + newUserId + "), email: " + contact);
	}

	private void send(String to, String subject, String text) {
		JavaMailSender sender = mailSender.getIfAvailable();
		if (sender == null) {
			log.info("[mail] chưa cấu hình SMTP, bỏ qua gửi tới {}", to);
			return;
		}
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			if (StringUtils.hasText(from)) {
				message.setFrom(from.trim());
			}
			message.setTo(to);
			message.setSubject(subject);
			message.setText(text);
			sender.send(message);
		} catch (RuntimeException ex) {
			log.warn("[mail] gửi thất bại tới {}: {}", to, ex.getMessage());
		}
	}

	static String resolveToAddress(String contactEmail, String username) {
		if (StringUtils.hasText(contactEmail)) {
			String e = contactEmail.trim();
			if (e.contains("@")) {
				return e;
			}
		}
		if (StringUtils.hasText(username) && username.trim().contains("@")) {
			return username.trim();
		}
		return null;
	}
}
