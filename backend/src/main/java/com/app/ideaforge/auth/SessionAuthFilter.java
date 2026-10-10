package com.app.ideaforge.auth;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.SecurityContextRepository;
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
	private final SecurityContextRepository securityContextRepository;

	public SessionAuthFilter(
			SessionService sessionService,
			SessionCookieService cookieService,
			SecurityContextRepository securityContextRepository) {
		this.sessionService = sessionService;
		this.cookieService = cookieService;
		this.securityContextRepository = securityContextRepository;
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
					SecurityContext context = SecurityContextHolder.createEmptyContext();
					context.setAuthentication(authentication);
					SecurityContextHolder.setContext(context);
					// Mark the context as established for this request. Without this,
					// SessionManagementFilter sees a "new" authentication and runs the
					// session strategies, which include CsrfAuthenticationStrategy and
					// would clear the CSRF cookie on every authenticated request.
					securityContextRepository.saveContext(context, request, response);
					if (resolution.extended()) {
						cookieService.refreshCookie(response, token);
					}
				});
			}
		}
		filterChain.doFilter(request, response);
	}
}
