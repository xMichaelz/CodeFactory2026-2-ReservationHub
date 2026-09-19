package co.udea.codefactory.reservahub.servicecatalog.service;

import co.udea.codefactory.reservahub.audit.AuditLogger;
import co.udea.codefactory.reservahub.provider.entity.Provider;
import co.udea.codefactory.reservahub.provider.service.ProviderService;
import co.udea.codefactory.reservahub.security.SecurityUtils;
import co.udea.codefactory.reservahub.servicecatalog.DTO.CreateServiceRequestDTO;
import co.udea.codefactory.reservahub.servicecatalog.DTO.ServiceResponseDTO;
import co.udea.codefactory.reservahub.servicecatalog.DTO.UpdateServiceRequestDTO;
import co.udea.codefactory.reservahub.servicecatalog.entity.CatalogService;
import co.udea.codefactory.reservahub.servicecatalog.mapper.CatalogServiceMapper;
import co.udea.codefactory.reservahub.servicecatalog.repository.CatalogServiceRepository;
import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import co.udea.codefactory.reservahub.shared.exception.BusinessException;
import co.udea.codefactory.reservahub.shared.exception.ConflictException;
import co.udea.codefactory.reservahub.shared.exception.ErrorCodes;
import co.udea.codefactory.reservahub.shared.exception.ForbiddenException;
import co.udea.codefactory.reservahub.shared.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CatalogServiceService {

	private final CatalogServiceRepository catalogServiceRepository;
	private final ProviderService providerService;
	private final CatalogServiceMapper mapper;
	private final AuditLogger auditLogger;

	public CatalogServiceService(CatalogServiceRepository catalogServiceRepository, ProviderService providerService,
			CatalogServiceMapper mapper, AuditLogger auditLogger) {
		this.catalogServiceRepository = catalogServiceRepository;
		this.providerService = providerService;
		this.mapper = mapper;
		this.auditLogger = auditLogger;
	}

	@Transactional
	public ServiceResponseDTO create(CreateServiceRequestDTO request) {
		Provider provider = currentProvider();
		String name = request.name().trim();
		if (catalogServiceRepository.existsByProviderIdAndNameIgnoreCase(provider.getId(), name)) {
			throw new ConflictException("A service with this name already exists for the provider");
		}

		CatalogService service = new CatalogService();
		service.setProviderId(provider.getId());
		service.setName(name);
		service.setDescription(blankToNull(request.description()));
		service.setDurationMinutes(request.durationMinutes());
		service.setPrice(request.price());
		service.setStatus(EntityStatus.ACTIVE);

		CatalogService saved = catalogServiceRepository.save(service);
		auditLogger.info("SERVICE_CREATED", "serviceId=" + saved.getId() + " providerId=" + provider.getId());
		return mapper.toResponse(saved);
	}

	@Transactional
	public ServiceResponseDTO update(UUID serviceId, UpdateServiceRequestDTO request) {
		CatalogService service = requireOwnedService(serviceId);
		String name = request.name().trim();
		if (catalogServiceRepository.existsByProviderIdAndNameIgnoreCaseAndIdNot(service.getProviderId(), name,
				serviceId)) {
			throw new ConflictException("A service with this name already exists for the provider");
		}

		service.setName(name);
		service.setDescription(blankToNull(request.description()));
		service.setDurationMinutes(request.durationMinutes());
		service.setPrice(request.price());

		CatalogService saved = catalogServiceRepository.save(service);
		auditLogger.info("SERVICE_UPDATED", "serviceId=" + saved.getId());
		return mapper.toResponse(saved);
	}

	@Transactional
	public ServiceResponseDTO deactivate(UUID serviceId) {
		CatalogService service = requireOwnedService(serviceId);
		if (service.getStatus() == EntityStatus.INACTIVE) {
			throw new BusinessException(ErrorCodes.CONFLICT, "Service is already inactive",
					HttpStatus.CONFLICT.value());
		}
		service.setStatus(EntityStatus.INACTIVE);
		CatalogService saved = catalogServiceRepository.save(service);
		auditLogger.info("SERVICE_DEACTIVATED", "serviceId=" + saved.getId());
		return mapper.toResponse(saved);
	}

	@Transactional(readOnly = true)
	public CatalogService requireActiveService(UUID serviceId) {
		CatalogService service = catalogServiceRepository.findById(serviceId)
				.orElseThrow(() -> new ResourceNotFoundException("Service not found"));
		if (service.getStatus() != EntityStatus.ACTIVE) {
			throw new BusinessException(ErrorCodes.BAD_REQUEST, "Service is inactive and cannot be used",
					HttpStatus.BAD_REQUEST.value());
		}
		return service;
	}

	private CatalogService requireOwnedService(UUID serviceId) {
		Provider provider = currentProvider();
		CatalogService service = catalogServiceRepository.findById(serviceId)
				.orElseThrow(() -> new ResourceNotFoundException("Service not found"));
		if (!service.getProviderId().equals(provider.getId())) {
			throw new ForbiddenException("You are not allowed to modify this service");
		}
		return service;
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
