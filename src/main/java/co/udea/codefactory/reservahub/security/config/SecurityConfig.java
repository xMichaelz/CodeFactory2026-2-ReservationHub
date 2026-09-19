package co.udea.codefactory.reservahub.security.config;

import co.udea.codefactory.reservahub.security.jwt.JwtAuthenticationFilter;
import co.udea.codefactory.reservahub.security.jwt.JwtProperties;
import co.udea.codefactory.reservahub.shared.dto.ApiErrorResponse;
import co.udea.codefactory.reservahub.shared.exception.ErrorCodes;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;
import java.util.UUID;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JsonMapper jsonMapper = JsonMapper.shared();

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/providers").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
						.requestMatchers("/api/v1/auth/logout").authenticated()
						.requestMatchers("/api/v1/services/**").hasRole("PROVIDER")
						.requestMatchers("/api/v1/schedules/**").hasRole("PROVIDER")
						.requestMatchers("/api/v1/resources/**").hasRole("PROVIDER")
						.anyRequest().authenticated()
				)
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, authException) ->
								writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCodes.UNAUTHORIZED,
										"Authentication required"))
						.accessDeniedHandler((request, response, accessDeniedException) ->
								writeError(response, HttpServletResponse.SC_FORBIDDEN, ErrorCodes.FORBIDDEN,
										"Access denied"))
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	private void writeError(HttpServletResponse response, int status, String code, String message) {
		try {
			response.setStatus(status);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			jsonMapper.writeValue(response.getOutputStream(),
					new ApiErrorResponse(code, message, List.of(), UUID.randomUUID().toString()));
		} catch (Exception ignored) {
			response.setStatus(status);
		}
	}
}
