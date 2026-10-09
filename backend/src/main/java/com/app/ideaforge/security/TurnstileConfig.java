package com.app.ideaforge.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.app.ideaforge.common.config.AppProperties;

@Configuration
public class TurnstileConfig {

	private static final Logger log = LoggerFactory.getLogger(TurnstileConfig.class);

	@Bean
	public TurnstileVerifier turnstileVerifier(AppProperties properties) {
		AppProperties.Auth.Turnstile turnstile = properties.auth().turnstile();
		if (!turnstile.enabled()) {
			log.warn("Turnstile verification is DISABLED. This is only acceptable in dev/test.");
			return (token, ip) -> true;
		}
		return new HttpTurnstileVerifier(turnstile.verifyUrl(), turnstile.secretKey());
	}
}
