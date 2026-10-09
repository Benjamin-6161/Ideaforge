package com.app.ideaforge.user;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Canonical form used for uniqueness and lookups: lowercase, trimmed, `+tag`
 * stripped, and dots removed for Gmail. The original address is kept for
 * sending mail.
 */
@Component
public class EmailNormalizer {

	private static final Set<String> DOT_INSENSITIVE_DOMAINS = Set.of("gmail.com", "googlemail.com");

	public String normalize(String email) {
		if (email == null) {
			return "";
		}
		String trimmed = email.trim();
		int at = trimmed.lastIndexOf('@');
		if (at <= 0 || at == trimmed.length() - 1) {
			return trimmed.toLowerCase(Locale.ROOT);
		}
		String local = trimmed.substring(0, at).toLowerCase(Locale.ROOT);
		String domain = trimmed.substring(at + 1).toLowerCase(Locale.ROOT);

		int plus = local.indexOf('+');
		if (plus >= 0) {
			local = local.substring(0, plus);
		}
		if (DOT_INSENSITIVE_DOMAINS.contains(domain)) {
			local = local.replace(".", "");
			domain = "gmail.com";
		}
		return local + "@" + domain;
	}

	public String domainOf(String email) {
		int at = email == null ? -1 : email.lastIndexOf('@');
		if (at < 0 || at == email.length() - 1) {
			return "";
		}
		return email.substring(at + 1).trim().toLowerCase(Locale.ROOT);
	}
}
