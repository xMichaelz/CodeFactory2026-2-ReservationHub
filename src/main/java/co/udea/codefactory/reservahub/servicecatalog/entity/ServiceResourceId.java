package co.udea.codefactory.reservahub.servicecatalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record ServiceResourceId(
		@Column(name = "service_id") UUID serviceId,
		@Column(name = "resource_id") UUID resourceId) implements Serializable {
}
