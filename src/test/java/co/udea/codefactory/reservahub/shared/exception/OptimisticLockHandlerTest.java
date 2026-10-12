package co.udea.codefactory.reservahub.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Dos ediciones simultáneas de la misma fila con {@code @Version}: la perdedora responde 409, no 500. */
class OptimisticLockHandlerTest {

	@RestController
	static class StaleController {
		@GetMapping("/stale")
		String stale() {
			throw new ObjectOptimisticLockingFailureException("Reservation", "id");
		}
	}

	@Test
	void optimisticLockFailureMapsTo409() throws Exception {
		MockMvc mvc = MockMvcBuilders.standaloneSetup(new StaleController())
				.setControllerAdvice(new GlobalExceptionHandler()).build();

		mvc.perform(get("/stale")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.errorCode").value(ErrorCodes.CONFLICT));
	}
}
