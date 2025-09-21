package co.za.learn.bridge.mail;

import java.util.Optional;
import java.util.Properties;

import co.za.learn.bridge.model.entity.lookups.AppConfig;
import co.za.learn.bridge.repository.AppConfigRepository;
import co.za.learn.bridge.utils.ConstantUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Configuration
public class EmailConfig {

  private final AppConfigRepository repository;

  @Autowired
  public EmailConfig(AppConfigRepository repository) {
    this.repository = repository;
  }

  @Bean
  public JavaMailSender getMailSender() {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

    mailSender.setHost("smtp.gmail.com");
    mailSender.setPort(587);
    mailSender.setUsername(
        Optional.ofNullable(repository.findByCode("MAIL_USER"))
            .map(AppConfig::getDescription)
            .orElse(ConstantUtil.MAIL_USER)
        );

    mailSender.setPassword(
            Optional.ofNullable(repository.findByCode("MAIL_PASS"))
                    .map(AppConfig::getDescription)
                    .orElse(ConstantUtil.MAIL_PASS));

    Properties javaMailProperties = new Properties();
    javaMailProperties.put("mail.smtp.starttls.enable", "true");
    javaMailProperties.put("mail.smtp.auth", "true");
    javaMailProperties.put("mail.transport.protocol", "smtp");
    javaMailProperties.put("mail.debug", "true");

    mailSender.setJavaMailProperties(javaMailProperties);
    return mailSender;
  }

  /** HTML Email config */
  @Bean
  public ITemplateResolver templateResolver() {
    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix("templates/");
    templateResolver.setSuffix(".html");
    templateResolver.setTemplateMode(TemplateMode.HTML);
    return templateResolver;
  }
}
