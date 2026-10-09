package com.app.ideaforge.security;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Input hygiene: Unicode-normalizes, strips control and zero-width
 * characters, removes prompt delimiter strings, collapses whitespace, and
 * enforces a hard length cap.
 */
@Component
public class InputSanitizer {

	private static final Pattern ZERO_WIDTH =
			Pattern.compile("[\\u200B-\\u200F\\u2028-\\u202F\\u2060-\\u206F\\uFEFF\\u00AD]");
	private static final Pattern CONTROL =
			Pattern.compile("[\\p{Cc}\\p{Cf}&&[^\\n\\t]]");
	private static final List<String> DELIMITERS = List.of(
			"<user_input>", "</user_input>",
			"<untrusted_web_content>", "</untrusted_web_content>",
			"<system>", "</system>", "<idea_update>", "</idea_update>");

	public String sanitizeSingleLine(String raw, int maxLength) {
		String sanitized = sanitize(raw, maxLength);
		if (sanitized == null) {
			return null;
		}
		return sanitized.replaceAll("\\s+", " ").trim();
	}

	public String sanitizeText(String raw, int maxLength) {
		String sanitized = sanitize(raw, maxLength);
		if (sanitized == null) {
			return null;
		}
		return sanitized.replaceAll("\\n{3,}", "\n\n").trim();
	}

	private String sanitize(String raw, int maxLength) {
		if (raw == null) {
			return null;
		}
		String value = Normalizer.normalize(raw, Normalizer.Form.NFKC);
		value = ZERO_WIDTH.matcher(value).replaceAll("");
		value = CONTROL.matcher(value).replaceAll("");
		for (String delimiter : DELIMITERS) {
			value = value.replace(delimiter, "");
			value = value.replace(delimiter.toUpperCase(java.util.Locale.ROOT), "");
		}
		if (value.length() > maxLength) {
			value = value.substring(0, maxLength);
		}
		return value;
	}
}
