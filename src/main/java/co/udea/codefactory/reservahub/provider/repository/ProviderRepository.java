package co.udea.codefactory.reservahub.provider.repository;

import co.udea.codefactory.reservahub.provider.entity.Provider;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProviderRepository extends JpaRepository<Provider, UUID> {

	boolean existsByBusinessNameIgnoreCase(String businessName);

	Optional<Provider> findByUserId(UUID userId);

	boolean existsByIdAndUserId(UUID id, UUID userId);

	/** {@code SELECT … FOR UPDATE}: serializa las reservas de un mismo proveedor (ADR-015). */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM Provider p WHERE p.id = :id")
	Optional<Provider> findByIdForUpdate(@Param("id") UUID id);
}
