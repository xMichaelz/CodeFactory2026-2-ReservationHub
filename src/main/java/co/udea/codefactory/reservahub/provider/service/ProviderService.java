package co.udea.codefactory.reservahub.provider.service;

import co.udea.codefactory.reservahub.audit.AuditLogger;
import co.udea.codefactory.reservahub.provider.DTO.CreateProviderRequestDTO;
import co.udea.codefactory.reservahub.provider.DTO.ProviderResponseDTO;
import co.udea.codefactory.reservahub.provider.entity.Provider;
import co.udea.codefactory.reservahub.provider.mapper.ProviderMapper;
import co.udea.codefactory.reservahub.provider.repository.ProviderRepository;
import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import co.udea.codefactory.reservahub.shared.exception.ConflictException;
import co.udea.codefactory.reservahub.shared.exception.ForbiddenException;
import co.udea.codefactory.reservahub.shared.exception.ResourceNotFoundException;
import co.udea.codefactory.reservahub.user.entity.User;
import co.udea.codefactory.reservahub.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProviderService {

	private final ProviderRepository providerRepository;
	private final UserService userService;
	private final ProviderMapper providerMapper;
	private final AuditLogger auditLogger;

	public ProviderService(ProviderRepository providerRepository, UserService userService,
			ProviderMapper providerMapper, AuditLogger auditLogger) {
		this.providerRepository = providerRepository;
		this.userService = userService;
		this.providerMapper = providerMapper;
		this.auditLogger = auditLogger;
	}

	@Transactional
	public ProviderResponseDTO register(CreateProviderRequestDTO request) {
		String businessName = request.businessName().trim();
		if (providerRepository.existsByBusinessNameIgnoreCase(businessName)) {
			throw new ConflictException("Business name is already registered");
		}

		User user = userService.createProviderAccount(
				request.email(),
				request.password(),
				request.firstName(),
				request.lastName(),
				request.phone()
		);

		Provider provider = new Provider();
		provider.setUserId(user.getId());
		provider.setBusinessName(businessName);
		provider.setDescription(blankToNull(request.description()));
		provider.setAddress(blankToNull(request.address()));
		provider.setStatus(EntityStatus.ACTIVE);

		Provider saved = providerRepository.save(provider);
		auditLogger.info("PROVIDER_REGISTERED", "providerId=" + saved.getId() + " userId=" + user.getId());
		return providerMapper.toResponse(saved, user);
	}

	@Transactional(readOnly = true)
	public Provider requireOwnedProvider(UUID providerId, UUID authenticatedUserId) {
		Provider provider = providerRepository.findById(providerId)
				.orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
		if (!provider.getUserId().equals(authenticatedUserId)) {
			throw new ForbiddenException("You are not allowed to act on behalf of this provider");
		}
		return provider;
	}

	@Transactional(readOnly = true)
	public Provider requireByUserId(UUID userId) {
		return providerRepository.findByUserId(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Provider profile not found for current user"));
	}

	@Transactional(readOnly = true)
	public Provider requireById(UUID providerId) {
		return providerRepository.findById(providerId)
				.orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
