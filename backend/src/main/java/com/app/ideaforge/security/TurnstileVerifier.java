package com.app.ideaforge.security;

/** Verifies Cloudflare Turnstile tokens; no-op when disabled in dev/test. */
public interface TurnstileVerifier {

	boolean verify(String token, String remoteIp);
}
