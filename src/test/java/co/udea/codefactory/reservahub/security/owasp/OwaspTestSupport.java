package co.udea.codefactory.reservahub.security.owasp;

import co.udea.codefactory.reservahub.security.SecurityUtils;
import co.udea.codefactory.reservahub.security.jwt.JwtService;
import co.udea.codefactory.reservahub.shared.enums.Role;
import co.udea.codefactory.reservahub.user.entity.User;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/** Utilidades comunes de las pruebas transversales OWASP (H3): descubrimiento de endpoints y peticiones. */
final class OwaspTestSupport {

	/** Raíz del proyecto (Surefire ejecuta con el directorio del módulo como user.dir). */
	static final Path ROOT = Path.of(System.getProperty("user.dir"));

	private OwaspTestSupport() {
	}

	static MockMvc mvc(ApplicationContext context) {
		return MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).apply(springSecurity()).build();
	}

	/** Endpoints propios de la app como "MÉTODO ruta" (los de springdoc/actuator quedan fuera). */
	static List<String> discoverEndpoints(ApplicationContext context) {
		RequestMappingHandlerMapping mapping = context.getBean("requestMappingHandlerMapping",
				RequestMappingHandlerMapping.class);
		Set<String> found = new TreeSet<>();
		for (Map.Entry<RequestMappingInfo, HandlerMethod> e : mapping.getHandlerMethods().entrySet()) {
			if (!e.getValue().getBeanType().getName().startsWith("co.udea.codefactory")) {
				continue;
			}
			for (String path : e.getKey().getPathPatternsCondition().getPatternValues()) {
				for (var method : e.getKey().getMethodsCondition().getMethods()) {
					found.add(method.name() + " " + path);
				}
			}
		}
		return new ArrayList<>(found);
	}

	static MockHttpServletRequestBuilder request(String key) {
		String[] parts = key.split(" ", 2);
		String path = parts[1].replaceAll("\\{[^}]+}", "00000000-0000-0000-0000-000000000000");
		return MockMvcRequestBuilders.request(HttpMethod.valueOf(parts[0]), path)
				.contentType(MediaType.APPLICATION_JSON).content("{}");
	}

	static RequestPostProcessor remoteAddr(String ip) {
		return req -> {
			req.setRemoteAddr(ip);
			return req;
		};
	}

	static String bearerFor(JwtService jwtService, UUID userId, Role role) {
		User u = new User();
		u.setId(userId);
		u.setEmail(userId + "@example.com");
		u.setRole(role);
		return "Bearer " + jwtService.generateToken(u);
	}

	static Authentication authenticationFor(UUID userId, Role role) {
		var principal = new SecurityUtils.AuthenticatedUser(userId, userId + "@example.com", role);
		return new UsernamePasswordAuthenticationToken(principal, null,
				List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
	}

	/** Estado HTTP de una petición; si la excepción escapa del handler (sin datos reales) se toma como 500. */
	static int status(MockMvc mvc, MockHttpServletRequestBuilder builder) {
		try {
			return mvc.perform(builder).andReturn().getResponse().getStatus();
		} catch (Exception ex) {
			return 500;
		}
	}

	static String read(String relative) throws IOException {
		return Files.readString(ROOT.resolve(relative));
	}

	/** application.properties de producción (el de test lo tapa en el classpath, por eso se lee del disco). */
	static Properties mainProperties() throws IOException {
		Properties p = new Properties();
		try (InputStream in = Files.newInputStream(ROOT.resolve("src/main/resources/application.properties"))) {
			p.load(in);
		}
		return p;
	}
}
