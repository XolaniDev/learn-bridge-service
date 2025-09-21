package co.za.learn.bridge.repository;

import co.za.learn.bridge.model.dto.ERole;
import co.za.learn.bridge.model.entity.Role;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoleRepository extends MongoRepository<Role, String> {
  Optional<Role> findByName(ERole name);

  boolean existsByName(ERole name);
}
