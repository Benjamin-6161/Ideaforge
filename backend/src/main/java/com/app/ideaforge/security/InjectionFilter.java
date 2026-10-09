package com.app.ideaforge.security;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Heuristic prompt-injection pre-filter. A speed bump, not the main defense;
 * layered rules live in the system prompts, output schemas, and tool policy.
 */
@Component
public class InjectionFilter {

	public record Result(boolean blocked, String reason) {
		public static Result allowed() {
			return new Result(false, null);
		}
	}

	private record Rule(Pattern pattern, String reason) {
	}

	private static final List<Rule> RULES = List.of(
			rule("ignore\\s+(all\\s+|any\\s+|the\\s+)?(previous|prior|above)\\s+(instructions|prompts?|rules)", "ignore_instructions"),
			rule("ignore\\s+(all|any|the)\\s+(instructions|rules|prompts)", "ignore_instructions"),
			rule("disregard\\s+(all\\s+|any\\s+|the\\s+)?(previous|prior|above)", "disregard_instructions"),
			rule("forget\\s+(everything|all|your)\\s*(previous|prior|above)?\\s*(instructions|rules|prompts)?", "forget_instructions"),
			rule("(reveal|show|print|repeat|output|leak|expose)\\s+(me\\s+)?(your|the)\\s+(system\\s+)?(prompt|instructions|rules|canary)", "reveal_prompt"),
			rule("what\\s+(are|were)\\s+your\\s+(system\\s+)?(instructions|rules|prompt)", "reveal_prompt"),
			rule("summari[sz]e\\s+(your\\s+|the\\s+)?(system\\s+)?(instructions|rules|prompt)", "reveal_prompt"),
			rule("you\\s+are\\s+now\\s+(a|an|the)?", "role_change"),
			rule("from\\s+now\\s+on,?\\s+you\\s+(are|will|must)", "role_change"),
			rule("(act|behave)\\s+as\\s+(a|an|if|though)", "role_play"),
			rule("pretend\\s+(to\\s+be|you\\s+are|that\\s+you)", "role_play"),
			rule("role[-\\s]?play\\s+as", "role_play"),
			rule("jailbreak|dan\\s+mode|developer\\s+mode|do\\s+anything\\s+now|unrestricted\\s+mode", "jailbreak"),
			rule("new\\s+(system\\s+)?(instructions|prompt|rules)\\s*(are|:)", "fake_system_message"),
			rule("^\\s*system\\s*:", "fake_system_message"),
			rule("<\\|im_start\\|>|<\\|im_end\\|>|<\\|system\\|>|\\[inst\\]|<<sys>>|###\\s*system", "fake_system_markers"),
			rule("override\\s+(your\\s+)?(rules|instructions|safety|guardrails)", "override_rules"),
			rule("bypass\\s+(your\\s+)?(rules|filters|safety|guardrails|restrictions)", "bypass_rules"),
			rule("do\\s+not\\s+follow\\s+(your|the)\\s+(rules|instructions)", "ignore_instructions"),
			rule("ignore\\s+(the\\s+)?(above|context|delimiters)", "ignore_instructions"),
			rule("(decode|base64|rot13|hex)[^.\\n]{0,40}(and\\s+)?(follow|execute|obey|instructions)", "encoded_payload"),
			rule("(execute|run)\\s+(the\\s+)?(following\\s+)?(code|command|script)", "code_execution"),
			rule("(read|open|access|cat)\\s+(the\\s+)?(/etc/|~/\\.ssh|id_rsa|/proc/|the\\s+file)", "file_access"),
			rule("call\\s+(the\\s+)?(search|fetch|browse|http)[a-z_]*\\s+tool", "tool_abuse"),
			rule("use\\s+your\\s+tools?\\s+to\\s+(access|read|fetch|download)", "tool_abuse"),
			rule("exfiltrate|extract\\s+(the\\s+)?(secrets?|credentials?|tokens?)", "exfiltration"),
			rule("send\\s+(this|the)\\s+(data|content|conversation|prompt)\\s+to", "exfiltration"),
			rule("translate[^.\\n]{0,60}(your\\s+)?(instructions|system\\s+prompt|rules)", "translation_trick"),
			rule("print\\s+verbatim|respond\\s+only\\s+with|output\\s+exactly", "output_manipulation"),
			rule("repeat\\s+after\\s+me", "output_manipulation"),
			rule("(disregard|ignore|remove)\\s+(the\\s+)?(delimiters?|tags?|blocks?)", "delimiter_breaking"),
			rule("</?\\s*(user_input|untrusted_web_content|system)\\s*>", "delimiter_breaking"));

	/** Long, high-entropy base64-looking blob: encoded instructions. */
	private static final Pattern BASE64_BLOB = Pattern.compile("[A-Za-z0-9+/]{160,}={0,2}");

	public Result check(String rawText) {
		if (rawText == null || rawText.isBlank()) {
			return Result.allowed();
		}
		String normalized = Normalizer.normalize(rawText, Normalizer.Form.NFKC)
				.toLowerCase(Locale.ROOT);
		for (Rule rule : RULES) {
			if (rule.pattern().matcher(normalized).find()) {
				return new Result(true, rule.reason());
			}
		}
		if (BASE64_BLOB.matcher(normalized).find()) {
			return new Result(true, "encoded_payload");
		}
		return Result.allowed();
	}

	private static Rule rule(String regex, String reason) {
		return new Rule(Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE), reason);
	}
}
