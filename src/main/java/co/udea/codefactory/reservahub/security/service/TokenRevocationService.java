package co.udea.codefactory.reservahub.security.service;

import co.udea.codefactory.reservahub.security.entity.RevokedToken;
import co.udea.codefactory.reservahub.security.repository.RevokedTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TokenRevocationService {

	private final RevokedTokenRepository revokedTokenRepository;

	public TokenRevocationService(RevokedTokenRepository revokedTokenRepository) {
		this.revokedTokenRepository = revokedTokenRepository;
	}

	@Transactional
	public void revoke(String jti, Instant expiresAt) {
		if (jti == null || jti.isBlank()) {
			return;
		}
		if (revokedTokenRepository.existsByJti(jti)) {
			return;
		}
		RevokedToken token = new RevokedToken();
		token.setJti(jti);
		token.setExpiresAt(expiresAt);
		revokedTokenRepository.save(token);
	}

	@Transactional(readOnly = true)
	public boolean isRevoked(String jti) {
		return jti != null && revokedTokenRepository.existsByJti(jti);
	}
}
