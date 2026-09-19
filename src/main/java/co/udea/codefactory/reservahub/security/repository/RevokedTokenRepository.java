package co.udea.codefactory.reservahub.security.repository;

import co.udea.codefactory.reservahub.security.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

	boolean existsByJti(String jti);

	@Modifying
	@Query("delete from RevokedToken r where r.expiresAt < :now")
	int deleteExpired(Instant now);
}
