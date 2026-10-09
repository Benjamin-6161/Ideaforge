package com.app.ideaforge.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/** Hashing and random-token helpers. Secrets are compared in constant time. */
@Component
public class HashingService {

	private static final SecureRandom RANDOM = new SecureRandom();

	public String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return toHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 unavailable", e);
		}
	}

	public String hmacSha256Hex(String pepper, String value) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return toHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new IllegalStateException("HmacSHA256 unavailable", e);
		}
	}

	public boolean constantTimeEquals(String a, String b) {
		if (a == null || b == null) {
			return false;
		}
		return MessageDigest.isEqual(
				a.getBytes(StandardCharsets.UTF_8),
				b.getBytes(StandardCharsets.UTF_8));
	}

	/** 256 bits of randomness, URL-safe, no padding. */
	public String randomToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** Numeric code of the given length, uniformly random. */
	public String randomDigits(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(RANDOM.nextInt(10));
		}
		return sb.toString();
	}

	private String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(Character.forDigit((b >> 4) & 0xF, 16));
			sb.append(Character.forDigit(b & 0xF, 16));
		}
		return sb.toString();
	}
}
