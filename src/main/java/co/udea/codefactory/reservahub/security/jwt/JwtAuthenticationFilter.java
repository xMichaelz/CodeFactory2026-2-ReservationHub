package co.udea.codefactory.reservahub.security.jwt;

import co.udea.codefactory.reservahub.security.SecurityUtils;
import co.udea.codefactory.reservahub.security.service.TokenRevocationService;
import co.udea.codefactory.reservahub.shared.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final TokenRevocationService tokenRevocationService;

	public JwtAuthenticationFilter(JwtService jwtService, TokenRevocationService tokenRevocationService) {
		this.jwtService = jwtService;
		this.tokenRevocationService = tokenRevocationService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = header.substring(7).trim();
		try {
			Claims claims = jwtService.parseClaims(token);
			String jti = jwtService.extractJti(claims);
			if (tokenRevocationService.isRevoked(jti)) {
				filterChain.doFilter(request, response);
				return;
			}

			UUID userId = jwtService.extractUserId(claims);
			Role role = jwtService.extractRole(claims);
			String email = claims.get("email", String.class);

			SecurityUtils.AuthenticatedUser principal = new SecurityUtils.AuthenticatedUser(userId, email, role);
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					principal,
					null,
					List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
			);
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (JwtException | IllegalArgumentException ex) {
			SecurityContextHolder.clearContext();
		}

		filterChain.doFilter(request, response);
	}
}
