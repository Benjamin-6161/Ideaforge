package com.app.ideaforge.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailNormalizerTest {

	private final EmailNormalizer normalizer = new EmailNormalizer();

	@Test
	void lowercasesAndTrims() {
		assertThat(normalizer.normalize("  User@Example.COM ")).isEqualTo("user@example.com");
	}

	@Test
	void stripsPlusTags() {
		assertThat(normalizer.normalize("user+newsletter@example.com")).isEqualTo("user@example.com");
		assertThat(normalizer.normalize("a.b+tag@other.io")).isEqualTo("a.b@other.io");
	}

	@Test
	void removesDotsForGmailAndCanonicalizesGooglemail() {
		assertThat(normalizer.normalize("First.Last@gmail.com")).isEqualTo("firstlast@gmail.com");
		assertThat(normalizer.normalize("first.last@googlemail.com")).isEqualTo("firstlast@gmail.com");
	}

	@Test
	void keepsDotsForOtherProviders() {
		assertThat(normalizer.normalize("first.last@outlook.com")).isEqualTo("first.last@outlook.com");
	}

	@Test
	void extractsDomain() {
		assertThat(normalizer.domainOf("User@Example.COM")).isEqualTo("example.com");
		assertThat(normalizer.domainOf("not-an-email")).isEqualTo("");
	}
}
