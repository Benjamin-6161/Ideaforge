package com.app.ideaforge.security;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.app.ideaforge.common.config.AppProperties;

import jakarta.annotation.PostConstruct;

/**
 * Disposable-email blocker. Loads a bundled open-source blocklist and
 * optionally refreshes it weekly, falling back to the bundled copy on failure.
 * Matches the domain and its parent domains. A configurable allowlist wins.
 */
@Component
public class DisposableEmailChecker {

	private static final Logger log = LoggerFactory.getLogger(DisposableEmailChecker.class);
	private static final String BUNDLED_LIST = "security/disposable-email-domains.txt";

	private volatile Set<String> blocklist = Set.of();
	private final Set<String> allowlist;
	private final AppProperties properties;

	public DisposableEmailChecker(AppProperties properties) {
		this.properties = properties;
		this.allowlist = parseAllowlist(properties.disposableEmail().allowlist());
	}

	@PostConstruct
	void loadBundled() {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				new ClassPathResource(BUNDLED_LIST).getInputStream(), StandardCharsets.UTF_8))) {
			this.blocklist = parseLines(reader.lines().toList());
			log.info("Loaded {} disposable email domains from bundled list", blocklist.size());
		} catch (Exception ex) {
			log.error("Could not load bundled disposable email list; blocking disabled", ex);
		}
	}

	@Scheduled(cron = "0 30 4 * * 1")
	public void refreshIfEnabled() {
		if (!properties.disposableEmail().refreshEnabled()) {
			return;
		}
		try {
			HttpClient client = HttpClient.newBuilder()
					.connectTimeout(Duration.ofSeconds(10))
					.build();
			HttpRequest request = HttpRequest.newBuilder(URI.create(properties.disposableEmail().refreshUrl()))
					.timeout(Duration.ofSeconds(30))
					.GET()
					.build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				log.warn("Disposable email list refresh failed with status {}", response.statusCode());
				return;
			}
			Set<String> refreshed = parseLines(Arrays.asList(response.body().split("\n")));
			if (refreshed.size() < 100) {
				log.warn("Refusing to replace blocklist with suspiciously small list of {}", refreshed.size());
				return;
			}
			this.blocklist = refreshed;
			log.info("Refreshed disposable email domains: {}", refreshed.size());
		} catch (Exception ex) {
			log.warn("Disposable email list refresh failed, keeping bundled copy: {}", ex.getMessage());
		}
	}

	public boolean isDisposable(String email) {
		String domain = domainOf(email);
		if (domain == null || domain.isBlank()) {
			return false;
		}
		if (matchesList(allowlist, domain)) {
			return false;
		}
		return matchesList(blocklist, domain);
	}

	private boolean matchesList(Set<String> list, String domain) {
		String candidate = domain;
		while (candidate.contains(".")) {
			if (list.contains(candidate)) {
				return true;
			}
			candidate = candidate.substring(candidate.indexOf('.') + 1);
		}
		return false;
	}

	private Set<String> parseAllowlist(String configured) {
		if (configured == null || configured.isBlank()) {
			return Set.of();
		}
		return Arrays.stream(configured.split(","))
				.map(s -> s.trim().toLowerCase(Locale.ROOT))
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toUnmodifiableSet());
	}

	private Set<String> parseLines(java.util.List<String> lines) {
		Set<String> domains = new HashSet<>();
		for (String line : lines) {
			String trimmed = line.trim().toLowerCase(Locale.ROOT);
			if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
				domains.add(trimmed);
			}
		}
		return Set.copyOf(domains);
	}

	private String domainOf(String email) {
		int at = email == null ? -1 : email.lastIndexOf('@');
		if (at < 0 || at == email.length() - 1) {
			return null;
		}
		return email.substring(at + 1).toLowerCase(Locale.ROOT);
	}
}
