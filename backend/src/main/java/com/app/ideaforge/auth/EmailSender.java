package com.app.ideaforge.auth;

/** Sends transactional email. SMTP implementation; no provider hardcoded. */
public interface EmailSender {

	void sendOtp(String toEmail, String code, int ttlMinutes);
}
