package co.udea.codefactory.reservahub.security.service;

import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import co.udea.codefactory.reservahub.user.entity.User;
import co.udea.codefactory.reservahub.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByEmailIgnoreCase(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
		boolean enabled = user.getStatus() == EntityStatus.ACTIVE;
		return new org.springframework.security.core.userdetails.User(
				user.getEmail(),
				user.getPasswordHash(),
				enabled,
				true,
				true,
				true,
				List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
		);
	}
}
