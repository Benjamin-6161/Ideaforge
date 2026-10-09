package com.app.ideaforge.auth;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Resolves our opaque session cookie into a Spring Security authentication. */
@Component
public class SessionAuthFilter extends OncePerRequestFilter {

	private final SessionService sessionService;
	private final SessionCookieService cookieService;

	public SessionAuthFilter(SessionService sessionService, SessionCookieService cookieService) {
		this.sessionService = sessionService;
		this.cookieService = cookieService;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			String token = cookieService.readToken(request);
			if (token != null) {
				sessionService.resolve(token).ifPresent(resolution -> {
					UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken
							.authenticated(
									resolution.user(),
									null,
									java.util.List.of(new SimpleGrantedAuthority("ROLE_" + resolution.user().role())));
					authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authentication);
					if (resolution.extended()) {
						cookieService.refreshCookie(response, token);
					}
				});
			}
		}
		filterChain.doFilter(request, response);
	}
}
