package com.app.ideaforge.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import com.app.ideaforge.common.config.AppProperties;

/**
 * Fails fast on unsafe production configuration (dev OTP, default secrets).
 */
@Component
public class ProdSafetyValidator implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ProdSafetyValidator.class);

	private final Environment environment;
	private final AppProperties properties;

	public ProdSafetyValidator(Environment environment, AppProperties properties) {
		this.environment = environment;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!environment.acceptsProfiles(Profiles.of("prod"))) {
			return;
		}
		requireSecret("app.security.session-pepper", properties.security().sessionPepper());
		requireSecret("app.security.otp-pepper", properties.security().otpPepper());
		requireSecret("app.security.ip-pepper", properties.security().ipPepper());
		if (properties.auth().devFixedOtp() != null && !properties.auth().devFixedOtp().isBlank()) {
			throw new IllegalStateException("app.auth.dev-fixed-otp must not be set under the prod profile");
		}
		if (properties.auth().turnstile().enabled()
				&& (properties.auth().turnstile().secretKey() == null
						|| properties.auth().turnstile().secretKey().isBlank())) {
			throw new IllegalStateException("Turnstile is enabled but app.auth.turnstile.secret-key is empty");
		}
		if (!properties.auth().session().cookieSecure()) {
			throw new IllegalStateException("Session cookies must be Secure under the prod profile");
		}
		log.info("Production safety checks passed");
	}

	private void requireSecret(String name, String value) {
		if (value == null || value.isBlank() || value.contains("dev-")) {
			throw new IllegalStateException(name + " must be set to a strong random value under the prod profile");
		}
	}
}
