package co.udea.codefactory.reservahub.resource.repository;

import co.udea.codefactory.reservahub.resource.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {

	boolean existsByProviderIdAndNameIgnoreCase(UUID providerId, String name);

	List<Resource> findByProviderIdOrderByNameAsc(UUID providerId);
}
