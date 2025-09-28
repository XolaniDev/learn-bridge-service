package co.za.learn.bridge.repository;

import co.za.learn.bridge.model.entity.User;
import jakarta.validation.constraints.NotBlank;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
  Optional<User> findByEmail(String email);
  Optional<User> findByEmailAndLearnerNumber(@NotBlank String email, @NotBlank String learnerNumber);
}
