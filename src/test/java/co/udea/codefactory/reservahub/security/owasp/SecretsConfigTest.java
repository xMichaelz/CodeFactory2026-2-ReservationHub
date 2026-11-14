package co.udea.codefactory.reservahub.security.owasp;

import co.udea.codefactory.reservahub.security.jwt.JwtProperties;
import co.udea.codefactory.reservahub.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.StandardEnvironment;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** V-01 · Sin secretos por defecto: ni en properties, ni en compose, y la app no arranca sin JWT_SECRET válido. */
class SecretsConfigTest {

	/** {@code ${VAR}} sin valor por defecto. */
	private static final Pattern ENV_NO_DEFAULT = Pattern.compile("^\\$\\{[A-Z0-9_]+}$");
	/** {@code ${VAR:?mensaje}}: compose falla si falta la variable. */
	private static final Pattern COMPOSE_REQUIRED = Pattern.compile("^\\$\\{[A-Z0-9_]+:\\?[^}]*}$");
	private static final List<String> SECRET_VARS = List.of("JWT_SECRET", "DB_PASSWORD", "POSTGRES_PASSWORD");

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(JwtProperties.class)
	@Import(JwtService.class)
	static class JwtOnly {
	}

	@Test
	void applicationPropertiesHaveNoDefaultSecrets() throws Exception {
		Properties p = OwaspTestSupport.mainProperties();
		List<String> failures = new ArrayList<>();
		for (String key : List.of("app.security.jwt.secret", "spring.datasource.password")) {
			String value = p.getProperty(key, "");
			if (!ENV_NO_DEFAULT.matcher(value.trim()).matches()) {
				failures.add(key + "=" + value);
			}
		}
		assertTrue(failures.isEmpty(), "Secretos con valor por defecto en application.properties: " + failures);
	}

	@Test
	@SuppressWarnings("unchecked")
	void composeHasNoLiteralSecrets() throws Exception {
		Map<String, Object> compose = new Yaml().load(OwaspTestSupport.read("compose.yaml"));
		Map<String, Object> services = (Map<String, Object>) compose.get("services");
		List<String> failures = new ArrayList<>();
		for (Map.Entry<String, Object> svc : services.entrySet()) {
			Map<String, String> env = environment(((Map<String, Object>) svc.getValue()).get("environment"));
			for (String var : SECRET_VARS) {
				String value = env.get(var);
				if (value != null && !COMPOSE_REQUIRED.matcher(value.trim()).matches()) {
					failures.add(svc.getKey() + "." + var + "=" + value);
				}
			}
		}
		assertTrue(failures.isEmpty(), "Secretos literales en compose.yaml (usar ${VAR:?…}): " + failures);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> environment(Object raw) {
		Map<String, String> env = new LinkedHashMap<>();
		if (raw instanceof Map<?, ?> m) {
			m.forEach((k, v) -> env.put(String.valueOf(k), v == null ? null : String.valueOf(v)));
		} else if (raw instanceof List<?> l) {
			for (Object item : l) {
				String[] kv = String.valueOf(item).split("=", 2);
				env.put(kv[0], kv.length > 1 ? kv[1] : null);
			}
		}
		return env;
	}

	/** Contexto con el application.properties real y sin variables de entorno ni del sistema. */
	private static ApplicationContextRunner runnerWithMainProperties(String... extra) throws Exception {
		Properties p = OwaspTestSupport.mainProperties();
		List<String> values = new ArrayList<>();
		p.stringPropertyNames().forEach(k -> values.add(k + "=" + p.getProperty(k)));
		values.addAll(List.of(extra));
		return new ApplicationContextRunner()
				.withInitializer(ctx -> {
					ctx.getEnvironment().getPropertySources()
							.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
					ctx.getEnvironment().getPropertySources()
							.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
				})
				.withPropertyValues(values.toArray(String[]::new))
				.withUserConfiguration(JwtOnly.class);
	}

	@Test
	void startupFailsWithoutJwtSecret() throws Exception {
		runnerWithMainProperties().run(ctx -> assertThat(ctx).hasFailed());
	}

	@Test
	void startupFailsWithAShortJwtSecret() throws Exception {
		runnerWithMainProperties("JWT_SECRET=too-short-secret").run(ctx -> assertThat(ctx).hasFailed());
	}

	@Test
	void startupSucceedsWithAStrongJwtSecret() throws Exception {
		runnerWithMainProperties("JWT_SECRET=a-strong-secret-with-more-than-thirty-two-bytes!!")
				.run(ctx -> assertThat(ctx).hasNotFailed());
	}
}
