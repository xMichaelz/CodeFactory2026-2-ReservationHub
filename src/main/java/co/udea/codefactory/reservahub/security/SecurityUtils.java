package co.udea.codefactory.reservahub.security;

import co.udea.codefactory.reservahub.shared.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

	private SecurityUtils() {
	}

	public static Optional<AuthenticatedUser> currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
			return Optional.empty();
		}
		return Optional.of(user);
	}

	public static AuthenticatedUser requireCurrentUser() {
		return currentUser().orElseThrow(() -> new IllegalStateException("No authenticated user in context"));
	}

	public record AuthenticatedUser(UUID userId, String email, Role role) implements java.security.Principal {
		@Override
		public String getName() {
			return email;
		}

		public boolean hasRole(Role expected) {
			return role == expected;
		}
	}

	public static boolean hasAuthority(Authentication authentication, String authority) {
		if (authentication == null) {
			return false;
		}
		for (GrantedAuthority granted : authentication.getAuthorities()) {
			if (authority.equals(granted.getAuthority())) {
				return true;
			}
		}
		return false;
	}
}
