package co.udea.codefactory.reservahub.security.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI reservahubOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("ReservaHub API")
						.description("Caso 14 CodeF@ctory UdeA — Sprint 1 (HU-01 a HU-10)")
						.version("v1")
						.contact(new Contact().name("Equipo Avanzado CodeF@ctory")))
				.components(new Components().addSecuritySchemes("bearerAuth",
						new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")));
	}
}
