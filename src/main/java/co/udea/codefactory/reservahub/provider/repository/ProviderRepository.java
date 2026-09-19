package co.udea.codefactory.reservahub.provider.repository;

import co.udea.codefactory.reservahub.provider.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProviderRepository extends JpaRepository<Provider, UUID> {

	boolean existsByBusinessNameIgnoreCase(String businessName);

	Optional<Provider> findByUserId(UUID userId);

	boolean existsByIdAndUserId(UUID id, UUID userId);
}
