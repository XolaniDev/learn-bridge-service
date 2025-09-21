package co.za.learn.bridge.repository;

import co.za.learn.bridge.model.entity.EmailContent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EmailContentRepository extends MongoRepository<EmailContent, String> {
    EmailContent findById(Long parseLong);
    List<EmailContent> findTop100ByOrderByIdDesc();
}
