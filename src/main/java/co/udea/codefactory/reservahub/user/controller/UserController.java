package co.udea.codefactory.reservahub.user.controller;

import co.udea.codefactory.reservahub.user.DTO.ClientLookupDTO;
import co.udea.codefactory.reservahub.user.DTO.CreateUserRequestDTO;
import co.udea.codefactory.reservahub.user.DTO.UserResponseDTO;
import co.udea.codefactory.reservahub.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Client registration (HU-01)")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/lookup")
	@Operation(summary = "Find an active client by email (providers only)")
	public ResponseEntity<ClientLookupDTO> lookup(@RequestParam String email) {
		return ResponseEntity.ok(userService.lookupClient(email));
	}

	@PostMapping
	@Operation(summary = "Register a client")
	public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody CreateUserRequestDTO request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerClient(request));
	}
}
