package co.udea.codefactory.reservahub.user.repository;

import co.udea.codefactory.reservahub.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

	boolean existsByEmailIgnoreCase(String email);

	Optional<User> findByEmailIgnoreCase(String email);
}
