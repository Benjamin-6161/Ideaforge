package com.app.ideaforge.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.app.ideaforge.common.config.AppProperties;

import static org.assertj.core.api.Assertions.assertThat;

class DisposableEmailCheckerTest {

	private DisposableEmailChecker checker;

	@BeforeEach
	void setUp() {
		AppProperties properties = new AppProperties(
				null, null, null, null, null,
				new AppProperties.DisposableEmail(false, "", "", false),
				null, null, null, null, null);
		checker = new DisposableEmailChecker(properties);
		checker.loadBundled();
	}

	@Test
	void blocksKnownDisposableDomains() {
		assertThat(checker.isDisposable("someone@mailinator.com")).isTrue();
		assertThat(checker.isDisposable("someone@guerrillamail.com")).isTrue();
	}

	@Test
	void blocksSubdomainsOfDisposableDomains() {
		assertThat(checker.isDisposable("someone@inbox.mailinator.com")).isTrue();
	}

	@Test
	void allowsRegularDomains() {
		assertThat(checker.isDisposable("someone@gmail.com")).isFalse();
		assertThat(checker.isDisposable("someone@acme-corp.example")).isFalse();
	}

	@Test
	void handlesMalformedInput() {
		assertThat(checker.isDisposable("")).isFalse();
		assertThat(checker.isDisposable("no-at-sign")).isFalse();
	}
}
