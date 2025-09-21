package co.za.learn.bridge.service;

import java.util.Date;
import java.util.List;

import co.za.learn.bridge.model.entity.EmailContent;
import co.za.learn.bridge.repository.EmailContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class EmailContentService {

  private final EmailContentRepository repository;

  @Autowired
  public EmailContentService(EmailContentRepository repository) {
    this.repository = repository;
  }

  @CacheEvict(value = "emails_content", allEntries = true)
  public void saveEmailContent(EmailContent emailContent) {
    if (emailContent.getId() == null) {
      emailContent.setCreateDate(new Date());
      emailContent.setDeleted(false);
    } else {
      emailContent.setLastUpdateDate(new Date());
    }
    repository.save(emailContent);
  }

  public EmailContent findById(Long parseLong) {
    return repository.findById(parseLong);
  }

  @CacheEvict(value = "emails_content", allEntries = true)
  public void deleteAllEmailContent(List<EmailContent> emailContents) {
    repository.deleteAll(emailContents);
  }

  public List<EmailContent> findTop100OrderByIdDesc() {
    return repository.findTop100ByOrderByIdDesc();
  }
}
