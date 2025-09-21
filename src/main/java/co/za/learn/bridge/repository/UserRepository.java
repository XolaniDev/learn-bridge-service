package co.za.learn.bridge.repository;

import co.za.learn.bridge.model.entity.User;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
  Optional<User> findByEmail(String email);
}
