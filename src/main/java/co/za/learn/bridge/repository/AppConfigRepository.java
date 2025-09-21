package co.za.learn.bridge.repository;

import co.za.learn.bridge.model.entity.lookups.AppConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AppConfigRepository extends MongoRepository<AppConfig, String> {
    AppConfig findByCode(String mailUser);
}
