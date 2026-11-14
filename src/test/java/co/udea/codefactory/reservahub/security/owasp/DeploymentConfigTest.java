package co.udea.codefactory.reservahub.security.owasp;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.ROOT;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V-07 · V-10 · V-11 · Despliegue: la BD y la API no se publican al host, imágenes fijadas por digest, sin root,
 * y dependencias vigiladas (Dependabot + OSV-Scanner + Trivy).
 */
class DeploymentConfigTest {

	@SuppressWarnings("unchecked")
	private static Map<String, Object> services(String file) throws IOException {
		Map<String, Object> compose = new Yaml().load(OwaspTestSupport.read(file));
		return (Map<String, Object>) compose.get("services");
	}

	@Test
	@Disabled("pendiente V-07 · O7")
	@SuppressWarnings("unchecked")
	void composeDoesNotPublishDbNorApp() throws Exception {
		Map<String, Object> services = services("compose.yaml");
		List<String> failures = new ArrayList<>();
		for (String name : List.of("db", "app")) {
			Map<String, Object> svc = (Map<String, Object>) services.get(name);
			if (svc != null && svc.get("ports") != null) {
				failures.add(name + " ports=" + svc.get("ports"));
			}
		}
		assertTrue(failures.isEmpty(), "compose.yaml publica puertos al host: " + failures);
	}

	@Test
	@SuppressWarnings("unchecked")
	void composeDevPublishesOnlyOnLoopback() throws Exception {
		if (!Files.exists(ROOT.resolve("compose.dev.yaml"))) {
			return;
		}
		List<String> failures = new ArrayList<>();
		for (Map.Entry<String, Object> svc : services("compose.dev.yaml").entrySet()) {
			Object ports = ((Map<String, Object>) svc.getValue()).get("ports");
			if (ports instanceof List<?> list) {
				for (Object port : list) {
					String text = port instanceof Map<?, ?> m ? String.valueOf(m.get("host_ip")) : String.valueOf(port);
					if (!text.startsWith("127.0.0.1")) {
						failures.add(svc.getKey() + " → " + port);
					}
				}
			}
		}
		assertTrue(failures.isEmpty(), "compose.dev.yaml debe publicar solo en 127.0.0.1: " + failures);
	}

	private static List<String> dockerfileLines() throws IOException {
		return OwaspTestSupport.read("Dockerfile").lines().map(String::trim)
				.filter(l -> !l.isEmpty() && !l.startsWith("#")).toList();
	}

	@Test
	@Disabled("pendiente V-10 · O7")
	void everyBaseImageIsPinnedByDigest() throws Exception {
		List<String> failures = dockerfileLines().stream()
				.filter(l -> l.toUpperCase().startsWith("FROM "))
				.filter(l -> !l.contains("@sha256:")).toList();
		assertTrue(failures.isEmpty(), "FROM sin @sha256: " + failures);
	}

	@Test
	@Disabled("pendiente V-10 · O7")
	void finalStageRunsAsNonRootUser() throws Exception {
		List<String> lines = dockerfileLines();
		int lastFrom = -1;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).toUpperCase().startsWith("FROM ")) {
				lastFrom = i;
			}
		}
		String user = null;
		for (String line : lines.subList(lastFrom + 1, lines.size())) {
			if (line.toUpperCase().startsWith("USER ")) {
				user = line.substring(5).trim();
			}
		}
		assertTrue(user != null && !user.equals("root") && !user.equals("0") && !user.startsWith("0:")
				&& !user.startsWith("root:"), "La etapa final del Dockerfile debe tener USER distinto de root: " + user);
	}

	@Test
	@Disabled("pendiente V-11 · O7")
	@SuppressWarnings("unchecked")
	void dependabotWatchesMavenDockerAndActions() throws Exception {
		Path file = ROOT.resolve(".github/dependabot.yml");
		assertTrue(Files.exists(file), "Falta .github/dependabot.yml");
		Map<String, Object> config = new Yaml().load(Files.readString(file));
		List<String> ecosystems = ((List<Map<String, Object>>) config.get("updates")).stream()
				.map(u -> String.valueOf(u.get("package-ecosystem"))).toList();
		assertTrue(ecosystems.containsAll(List.of("maven", "docker", "github-actions")),
				"dependabot.yml debe vigilar maven, docker y github-actions: " + ecosystems);
	}

	@Test
	@Disabled("pendiente V-11 · O7")
	void someWorkflowRunsOsvScannerAndTrivy() throws Exception {
		Path dir = ROOT.resolve(".github/workflows");
		StringBuilder all = new StringBuilder();
		if (Files.isDirectory(dir)) {
			try (Stream<Path> files = Files.list(dir)) {
				for (Path f : files.filter(p -> p.toString().matches(".*\\.ya?ml$")).toList()) {
					all.append(Files.readString(f).toLowerCase()).append('\n');
				}
			}
		}
		assertTrue(all.indexOf("osv-scanner") >= 0, "Ningún workflow ejecuta osv-scanner");
		assertTrue(all.indexOf("trivy") >= 0, "Ningún workflow ejecuta trivy");
	}
}
