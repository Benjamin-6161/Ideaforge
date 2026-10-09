package com.app.ideaforge.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.app.ideaforge.common.config.AppProperties;
import com.app.ideaforge.credits.CreditService;
import com.app.ideaforge.credits.CreditWalletRepository;
import com.app.ideaforge.user.EmailNormalizer;
import com.app.ideaforge.user.User;
import com.app.ideaforge.user.UserRepository;
import com.app.ideaforge.user.UserRole;

/**
 * Dev-only seed: three verified test users with credits. Never runs under
 * prod/test; production migrations contain no test users.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

	private final UserRepository users;
	private final CreditService creditService;
	private final CreditWalletRepository wallets;
	private final EmailNormalizer emailNormalizer;
	private final AppProperties properties;

	public DevDataSeeder(
			UserRepository users,
			CreditService creditService,
			CreditWalletRepository wallets,
			EmailNormalizer emailNormalizer,
			AppProperties properties) {
		this.users = users;
		this.creditService = creditService;
		this.wallets = wallets;
		this.emailNormalizer = emailNormalizer;
		this.properties = properties;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!properties.seed().enabled() || !properties.seed().testUsers()) {
			return;
		}
		for (int i = 1; i <= 3; i++) {
			String email = "test" + i + "@ideaforge.test";
			String normalized = emailNormalizer.normalize(email);
			int index = i;
			User user = users.findByEmailNormalized(normalized).orElseGet(() -> {
				User created = new User(email, normalized, null);
				created.setDisplayName("Test User " + index);
				created.setEmailVerifiedAt(java.time.Instant.now());
				created.setRole(index == 1 ? UserRole.ADMIN : UserRole.USER);
				return users.save(created);
			});
			wallets.ensureExists(user.getId(), java.time.Instant.now());
			creditService.grantAdmin(
					user.getId(),
					properties.seed().testUserCreditsUnits(),
					"dev-seed",
					"Dev test user credits");
		}
		log.info("Dev test users ready: test1..test3@ideaforge.test (OTP 000000)");
	}
}
