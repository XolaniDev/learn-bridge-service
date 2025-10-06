package co.za.learn.bridge.repository;

import co.za.learn.bridge.model.entity.Recommendations;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RecommendationsRepository extends MongoRepository<Recommendations, String> {
    Optional<Recommendations> findByUserId(String userId);
}
