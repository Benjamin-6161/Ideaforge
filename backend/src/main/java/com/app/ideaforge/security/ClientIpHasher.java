package com.app.ideaforge.security;

import org.springframework.stereotype.Component;

import com.app.ideaforge.common.config.AppProperties;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the client IP and turns it into a peppered HMAC hash. Raw IPs are
 * never stored or logged. X-Forwarded-For is trusted only when explicitly
 * configured (behind Caddy in prod).
 */
@Component
public class ClientIpHasher {

	private final HashingService hashingService;
	private final AppProperties properties;

	public ClientIpHasher(HashingService hashingService, AppProperties properties) {
		this.hashingService = hashingService;
		this.properties = properties;
	}

	public String hash(HttpServletRequest request) {
		return hash(resolveIp(request));
	}

	public String hash(String ip) {
		if (ip == null || ip.isBlank()) {
			ip = "unknown";
		}
		return hashingService.hmacSha256Hex(properties.security().ipPepper(), "ip:" + ip);
	}

	public String resolveIp(HttpServletRequest request) {
		if (properties.security().trustProxy()) {
			String forwarded = request.getHeader("X-Forwarded-For");
			if (forwarded != null && !forwarded.isBlank()) {
				// First entry is the original client; the rest may be attacker-supplied.
				String first = forwarded.split(",")[0].trim();
				if (!first.isEmpty()) {
					return first;
				}
			}
		}
		String remote = request.getRemoteAddr();
		return remote == null ? "unknown" : remote;
	}
}
