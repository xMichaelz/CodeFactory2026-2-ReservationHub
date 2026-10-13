package co.udea.codefactory.reservahub.servicecatalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_resources")
public class ServiceResource {

	@EmbeddedId
	private ServiceResourceId id;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ServiceResource() {
	}

	public ServiceResource(UUID serviceId, UUID resourceId) {
		this.id = new ServiceResourceId(serviceId, resourceId);
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public ServiceResourceId getId() {
		return id;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
