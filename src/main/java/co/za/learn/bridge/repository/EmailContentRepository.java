package co.za.learn.bridge.repository;

import co.za.learn.bridge.model.entity.EmailContent;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmailContentRepository extends MongoRepository<EmailContent, String> {
    EmailContent findById(Long parseLong);
    List<EmailContent> findTop100ByOrderByIdDesc();
}
