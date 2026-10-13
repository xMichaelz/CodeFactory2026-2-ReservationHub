package co.udea.codefactory.reservahub.servicecatalog.repository;

import co.udea.codefactory.reservahub.servicecatalog.DTO.CatalogEntryDTO;
import co.udea.codefactory.reservahub.servicecatalog.entity.CatalogService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogServiceRepository extends JpaRepository<CatalogService, UUID> {

	boolean existsByProviderIdAndNameIgnoreCase(UUID providerId, String name);

	boolean existsByProviderIdAndNameIgnoreCaseAndIdNot(UUID providerId, String name, UUID id);

	List<CatalogService> findByProviderIdOrderByNameAsc(UUID providerId);

	Optional<CatalogService> findByIdAndProviderId(UUID id, UUID providerId);

	/** Servicios activos de proveedores activos, ordenados por proveedor y nombre. */
	@Query("""
			SELECT new co.udea.codefactory.reservahub.servicecatalog.DTO.CatalogEntryDTO(
			    s.id, s.name, s.description, s.durationMinutes, s.price, p.id, p.businessName)
			FROM CatalogService s, Provider p
			WHERE s.providerId = p.id
			  AND s.status = co.udea.codefactory.reservahub.shared.enums.EntityStatus.ACTIVE
			  AND p.status = co.udea.codefactory.reservahub.shared.enums.EntityStatus.ACTIVE
			ORDER BY p.businessName, s.name
			""")
	List<CatalogEntryDTO> findActiveCatalog();
}
