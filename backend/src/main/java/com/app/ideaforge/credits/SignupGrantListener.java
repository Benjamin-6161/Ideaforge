package com.app.ideaforge.credits;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.app.ideaforge.user.UserVerifiedEvent;

/** Grants the signup credits once per user, in the signup transaction. */
@Component
public class SignupGrantListener {

	private final CreditService creditService;

	public SignupGrantListener(CreditService creditService) {
		this.creditService = creditService;
	}

	@EventListener
	public void onUserVerified(UserVerifiedEvent event) {
		if (event.newUser()) {
			creditService.grantSignup(event.userId());
		}
	}
}
