package co.udea.codefactory.reservahub.user.DTO;

import java.util.UUID;

/** Resultado mínimo de buscar un cliente por email: no expone el email ni otros datos. */
public record ClientLookupDTO(UUID id, String name) {
}
