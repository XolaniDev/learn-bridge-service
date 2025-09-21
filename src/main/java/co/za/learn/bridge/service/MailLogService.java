package co.za.learn.bridge.service;

import java.util.Date;

import co.za.learn.bridge.model.entity.EmailLog;
import co.za.learn.bridge.repository.EmailLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MailLogService {
  private final EmailLogRepository repository;

  @Autowired
  public MailLogService(EmailLogRepository repository) {
    this.repository = repository;
  }

  public void saveMailLog(EmailLog emailLog) {
    if (emailLog.getId() == null) {
      emailLog.setCreateDate(new Date());
    } else {
      emailLog.setLastUpdateDate(new Date());
    }
    repository.save(emailLog);
  }

}
