package co.za.learn.bridge.repository;

import co.za.learn.bridge.model.entity.UserFundings;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserFundingsRepository extends MongoRepository<UserFundings, String> {
  List<UserFundings> findByUserId(String userId);
}
