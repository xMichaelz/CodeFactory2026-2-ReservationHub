package co.udea.codefactory.reservahub.security.jwt;

import co.udea.codefactory.reservahub.shared.enums.Role;
import co.udea.codefactory.reservahub.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtService {

	private final JwtProperties properties;
	private final SecretKey secretKey;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
		byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
		}
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
	}

	public String generateToken(User user) {
		Instant now = Instant.now();
		Instant expiry = now.plusMillis(properties.getExpirationMs());
		return Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(user.getId().toString())
				.claim("email", user.getEmail())
				.claim("role", user.getRole().name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiry))
				.signWith(secretKey)
				.compact();
	}

	public Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	public UUID extractUserId(Claims claims) {
		return UUID.fromString(claims.getSubject());
	}

	public Role extractRole(Claims claims) {
		return Role.valueOf(claims.get("role", String.class));
	}

	public String extractJti(Claims claims) {
		return claims.getId();
	}

	public Instant extractExpiration(Claims claims) {
		return claims.getExpiration().toInstant();
	}

	public long getExpirationMs() {
		return properties.getExpirationMs();
	}
}
