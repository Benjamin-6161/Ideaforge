package com.app.ideaforge.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.app.ideaforge.common.config.AppProperties;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Component
public class SmtpEmailSender implements EmailSender {

	private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

	private final JavaMailSender mailSender;
	private final AppProperties properties;

	public SmtpEmailSender(JavaMailSender mailSender, AppProperties properties) {
		this.mailSender = mailSender;
		this.properties = properties;
	}

	@Override
	public void sendOtp(String toEmail, String code, int ttlMinutes) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
			helper.setFrom(new InternetAddress(
					properties.email().from(),
					properties.email().fromName()));
			helper.setTo(toEmail);
			helper.setSubject("Your IdeaForge login code: " + code);
			helper.setText("""
					Your IdeaForge login code is: %s

					It expires in %d minutes. If you did not request this, you can ignore this email.
					""".formatted(code, ttlMinutes));
			mailSender.send(message);
		} catch (Exception ex) {
			// Never fail the request because mail delivery failed; the code is stored.
			log.error("Failed to send OTP email: {}", ex.getMessage());
		}
	}
}
