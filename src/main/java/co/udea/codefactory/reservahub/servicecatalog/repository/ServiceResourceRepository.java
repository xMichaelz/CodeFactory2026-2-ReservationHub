package co.udea.codefactory.reservahub.servicecatalog.repository;

import co.udea.codefactory.reservahub.servicecatalog.entity.ServiceResource;
import co.udea.codefactory.reservahub.servicecatalog.entity.ServiceResourceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ServiceResourceRepository extends JpaRepository<ServiceResource, ServiceResourceId> {

	boolean existsByIdServiceIdAndIdResourceIdIn(UUID serviceId, Collection<UUID> resourceIds);

	List<ServiceResource> findByIdServiceId(UUID serviceId);

	List<ServiceResource> findByIdServiceIdIn(Collection<UUID> serviceIds);
}
