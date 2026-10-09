package com.app.ideaforge.user;

import java.util.UUID;

/** Published when an account is created or verified; used for the signup credit grant. */
public record UserVerifiedEvent(UUID userId, boolean newUser) {
}
