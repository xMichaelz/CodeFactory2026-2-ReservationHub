package co.udea.codefactory.reservahub.resource.service;

import co.udea.codefactory.reservahub.audit.AuditLogger;
import co.udea.codefactory.reservahub.provider.entity.Provider;
import co.udea.codefactory.reservahub.provider.service.ProviderService;
import co.udea.codefactory.reservahub.resource.DTO.CreateResourceRequestDTO;
import co.udea.codefactory.reservahub.resource.DTO.ResourceResponseDTO;
import co.udea.codefactory.reservahub.resource.entity.Resource;
import co.udea.codefactory.reservahub.resource.mapper.ResourceMapper;
import co.udea.codefactory.reservahub.resource.repository.ResourceRepository;
import co.udea.codefactory.reservahub.security.SecurityUtils;
import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import co.udea.codefactory.reservahub.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceService {

	private final ResourceRepository resourceRepository;
	private final ProviderService providerService;
	private final ResourceMapper mapper;
	private final AuditLogger auditLogger;

	public ResourceService(ResourceRepository resourceRepository, ProviderService providerService,
			ResourceMapper mapper, AuditLogger auditLogger) {
		this.resourceRepository = resourceRepository;
		this.providerService = providerService;
		this.mapper = mapper;
		this.auditLogger = auditLogger;
	}

	@Transactional
	public ResourceResponseDTO create(CreateResourceRequestDTO request) {
		Provider provider = currentProvider();
		String name = request.name().trim();
		if (resourceRepository.existsByProviderIdAndNameIgnoreCase(provider.getId(), name)) {
			throw new ConflictException("A resource with this name already exists for the provider");
		}

		Resource resource = new Resource();
		resource.setProviderId(provider.getId());
		resource.setName(name);
		resource.setResourceType(request.resourceType().trim().toUpperCase());
		resource.setDescription(blankToNull(request.description()));
		resource.setStatus(EntityStatus.ACTIVE);

		Resource saved = resourceRepository.save(resource);
		auditLogger.info("RESOURCE_CREATED", "resourceId=" + saved.getId() + " providerId=" + provider.getId());
		return mapper.toResponse(saved);
	}

	private Provider currentProvider() {
		SecurityUtils.AuthenticatedUser user = SecurityUtils.requireCurrentUser();
		return providerService.requireByUserId(user.userId());
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
