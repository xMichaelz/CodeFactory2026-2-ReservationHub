package co.udea.codefactory.reservahub.servicecatalog.repository;

import co.udea.codefactory.reservahub.servicecatalog.entity.CatalogService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CatalogServiceRepository extends JpaRepository<CatalogService, UUID> {

	boolean existsByProviderIdAndNameIgnoreCase(UUID providerId, String name);

	boolean existsByProviderIdAndNameIgnoreCaseAndIdNot(UUID providerId, String name, UUID id);

	Optional<CatalogService> findByIdAndProviderId(UUID id, UUID providerId);
}
