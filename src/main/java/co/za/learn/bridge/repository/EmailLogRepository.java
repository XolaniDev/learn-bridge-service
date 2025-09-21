package co.za.learn.bridge.repository;

import co.za.learn.bridge.model.entity.EmailLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmailLogRepository extends MongoRepository<EmailLog, String> {

}
