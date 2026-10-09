package com.app.ideaforge.auth.dto;

/** Bootstrap for the SPA: the cookie-backed CSRF token and header name. */
public record CsrfResponse(String token, String headerName) {
}
