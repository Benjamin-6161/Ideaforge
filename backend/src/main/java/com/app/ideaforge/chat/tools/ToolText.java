package com.app.ideaforge.chat.tools;

import java.util.regex.Pattern;

/** Defense in depth for tool output: strip prompt delimiters and cap length. */
final class ToolText {

	private static final Pattern DELIMITER_TAGS = Pattern.compile(
			"</?(?:user_input|untrusted_web_content|system|idea_update)>",
			Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

	private ToolText() {
	}

	static String sanitize(String value, int maxChars) {
		if (value == null) {
			return "";
		}
		String cleaned = DELIMITER_TAGS.matcher(value).replaceAll("");
		return cleaned.length() <= maxChars ? cleaned : cleaned.substring(0, maxChars);
	}
}
