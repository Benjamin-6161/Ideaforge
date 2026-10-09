package com.app.ideaforge.security;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import com.app.ideaforge.common.config.AppProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Checks that a domain can receive mail (MX, falling back to A). Fails open on
 * transient DNS errors so a resolver outage cannot block legitimate signups.
 */
@Component
public class MxChecker {

	private static final Logger log = LoggerFactory.getLogger(MxChecker.class);

	private final Cache<String, Boolean> cache = Caffeine.newBuilder()
			.maximumSize(10_000)
			.expireAfterWrite(Duration.ofHours(6))
			.build();

	private final AppProperties properties;

	public MxChecker(AppProperties properties) {
		this.properties = properties;
	}

	public boolean hasMailTarget(String domain) {
		if (!properties.disposableEmail().mxCheckEnabled()) {
			return true;
		}
		if (domain == null || domain.isBlank()) {
			return false;
		}
		return Boolean.TRUE.equals(cache.get(domain.toLowerCase(), this::lookup));
	}

	private boolean lookup(String domain) {
		try {
			Lookup mx = new Lookup(domain, Type.MX);
			mx.setSearchPath((String[]) null);
			Record[] mxRecords = mx.run();
			if (mxRecords != null && mxRecords.length > 0) {
				return true;
			}
			if (mx.getResult() == Lookup.TRY_AGAIN) {
				return true;
			}
			Lookup a = new Lookup(domain, Type.A);
			a.setSearchPath((String[]) null);
			Record[] aRecords = a.run();
			if (aRecords != null && aRecords.length > 0) {
				return true;
			}
			return a.getResult() == Lookup.TRY_AGAIN;
		} catch (Exception ex) {
			log.debug("DNS lookup failed for {}: {}", domain, ex.getMessage());
			return true;
		}
	}
}
