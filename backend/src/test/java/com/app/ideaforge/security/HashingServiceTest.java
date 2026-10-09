package com.app.ideaforge.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashingServiceTest {

	private final HashingService hashingService = new HashingService();

	@Test
	void hmacIsDeterministicPerPepper() {
		String a = hashingService.hmacSha256Hex("pepper-a", "value");
		String b = hashingService.hmacSha256Hex("pepper-a", "value");
		String c = hashingService.hmacSha256Hex("pepper-b", "value");
		assertThat(a).isEqualTo(b).isNotEqualTo(c).hasSize(64);
	}

	@Test
	void constantTimeEqualsHandlesNulls() {
		assertThat(hashingService.constantTimeEquals("abc", "abc")).isTrue();
		assertThat(hashingService.constantTimeEquals("abc", "abd")).isFalse();
		assertThat(hashingService.constantTimeEquals(null, "abc")).isFalse();
		assertThat(hashingService.constantTimeEquals("abc", null)).isFalse();
	}

	@Test
	void randomTokensAreUrlSafeAndUnique() {
		String first = hashingService.randomToken();
		String second = hashingService.randomToken();
		assertThat(first).isNotEqualTo(second).doesNotContain("+", "/", "=");
	}

	@Test
	void randomDigitsHasRequestedLength() {
		assertThat(hashingService.randomDigits(6)).matches("\\d{6}");
	}
}
