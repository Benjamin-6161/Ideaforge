package com.app.ideaforge.auth;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.app.ideaforge.common.config.AppProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Reads/writes the HttpOnly session cookie. */
@Component
public class SessionCookieService {

	private final AppProperties properties;

	public SessionCookieService(AppProperties properties) {
		this.properties = properties;
	}

	public String readToken(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		String name = properties.auth().session().cookieName();
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	public void writeCookie(HttpServletResponse response, String token, Duration maxAge) {
		ResponseCookie cookie = ResponseCookie.from(properties.auth().session().cookieName(), token)
				.httpOnly(true)
				.secure(properties.auth().session().cookieSecure())
				.sameSite(properties.auth().session().cookieSameSite())
				.path("/")
				.maxAge(maxAge)
				.build();
		response.addHeader("Set-Cookie", cookie.toString());
	}

	public void clearCookie(HttpServletResponse response) {
		ResponseCookie cookie = ResponseCookie.from(properties.auth().session().cookieName(), "")
				.httpOnly(true)
				.secure(properties.auth().session().cookieSecure())
				.sameSite(properties.auth().session().cookieSameSite())
				.path("/")
				.maxAge(0)
				.build();
		response.addHeader("Set-Cookie", cookie.toString());
	}

	/** Re-sends the cookie with a fresh max-age after sliding-expiry extension. */
	public void refreshCookie(HttpServletResponse response, String token) {
		writeCookie(response, token, Duration.ofDays(properties.auth().session().idleDays()));
	}
}
