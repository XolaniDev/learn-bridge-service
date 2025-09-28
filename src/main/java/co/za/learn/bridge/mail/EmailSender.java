package co.za.learn.bridge.mail;

import co.za.learn.bridge.model.entity.EmailContent;
import co.za.learn.bridge.model.entity.EmailLog;
import co.za.learn.bridge.service.EmailContentService;
import co.za.learn.bridge.service.MailLogService;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
public class EmailSender {

  public static final String ERROR_LOG = "ERROR: ";
  protected final Log logger = LogFactory.getLog(this.getClass());
  private final TemplateEngine templateEngine;
  private final MailLogService mailLogService;
  private final EmailContentService emailContentService;
  private final JavaMailSender emailSender;

  @Autowired
  public EmailSender(
      TemplateEngine templateEngine,
      MailLogService mailLogService,
      EmailContentService emailContentService,
      JavaMailSender emailSender) {
    this.templateEngine = templateEngine;
    this.mailLogService = mailLogService;
    this.emailContentService = emailContentService;
    this.emailSender = emailSender;
  }

  @Async
  public void saveEmail(Email email) {
    try {

      EmailContent emailContent =
          EmailContent.builder()
              .fromEmail(email.getFrom())
              .toEmail(getToEmails(email))
              .ccEmails(getCCEmails(email))
              .subject(email.getSubject())
              .contentMssg(email.getContent())
              .attachments(email.getAttachments())
              .errorMessage("")
              .build();
      emailContentService.saveEmailContent(emailContent);
    } catch (Exception e) {
      logger.error(ERROR_LOG, e);
    }
  }

  public void sendHtmlEmil(Email email) throws MailSenderException {

    logger.info("***************SEND HTML EMIL*****************");
    try {
      MimeMessage message = emailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true);

      helper.setSubject(email.getSubject());
      helper.setText(generateMailHtml(email.getContent(), email.getSubject()), true);
      helper.setTo(email.getTo());
      helper.setFrom(email.getFrom());
      if (email.getCc() != null && email.getCc().length > 0 && !email.getCc()[0].isEmpty()) {
        helper.setCc(email.getCc());
      }

      Optional.ofNullable(email.getAttachments())
              .filter(list -> !list.isEmpty())
              .ifPresent(attachments -> attachments.forEach(pdf -> {
                try {
                  File tempFile = File.createTempFile(pdf.getFileName(), pdf.getContentType());
                  Files.write(tempFile.toPath(), pdf.getData());
                  FileSystemResource resource = new FileSystemResource(tempFile);
                  helper.addAttachment(resource.getFilename(), resource.getFile());
                } catch (Exception e) {
                  logger.error("Error attaching PDF to email", e);
                }
              }));


      emailSender.send(message);
      EmailLog emailLog =
          new EmailLog(
              email.getFrom(),
              getToEmails(email),
              getCCEmails(email),
              email.getSubject(),
              email.getContent(),
              false,
              "");
      mailLogService.saveMailLog(emailLog);
    } catch (Exception e) {
      try {
        EmailLog emailLog =
            new EmailLog(
                email.getFrom(),
                getToEmails(email),
                getCCEmails(email),
                email.getSubject(),
                email.getContent(),
                true,
                e.getMessage());
        mailLogService.saveMailLog(emailLog);
        logger.info("");
      } catch (Exception e1) {
        logger.error(ERROR_LOG, e);
      }
      logger.error(ERROR_LOG, e);
      throw new MailSenderException(
          "We are unable to send email at the moment. Error Message: ".concat(e.getMessage()));
    }
  }

  public void sendEmailContent() {
    try {
      List<EmailContent> emailContentList = emailContentService.findTop100OrderByIdDesc();
      List<EmailContent> sentEmailContentList = new ArrayList<>();
      if (!emailContentList.isEmpty()) {
        for (EmailContent emailContent : emailContentList) {
          Email email = buildMail(emailContent);
          sendEmail(emailContent, email, sentEmailContentList);
        }
        emailContentService.deleteAllEmailContent(sentEmailContentList);
        deleteFarmTempFiles();
      }
    } catch (Exception e) {
      logger.info("Error when sending emails: ".concat(e.getMessage()));
    }
  }

  public void deleteFarmTempFiles() {
    String tempDir = System.getProperty("java.io.tmpdir");
    File tempFolder = new File(tempDir);

    if (!tempFolder.exists() || !tempFolder.isDirectory()) {
      System.out.println("Temp directory not found: " + tempDir);
      return;
    }

    File[] farmFiles = tempFolder.listFiles(file -> file.isFile() && file.getName().startsWith("farm-"));

    if (farmFiles != null) {
      for (File file : farmFiles) {
        if (file.delete()) {
          System.out.println("Deleted: " + file.getAbsolutePath());
        } else {
          System.out.println("Failed to delete: " + file.getAbsolutePath());
        }
      }
    }
  }


  private void sendEmail(
      EmailContent emailContent, Email email, List<EmailContent> sentEmailContentList) {
    try {
      sendHtmlEmil(email);
      sentEmailContentList.add(emailContent);
    } catch (Exception e) {
      logger.info(
          "Error when sending email with ID " + emailContent.getId() + ": ".concat(e.getMessage()));
      if (e.getMessage() != null && e.getMessage().contains("is not a valid RFC-5321 address")) {
        EmailLog emailLog =
            new EmailLog(
                email.getFrom(),
                getToEmails(email),
                getCCEmails(email),
                email.getSubject(),
                email.getContent(),
                true,
                "Error Message: ".concat(e.getMessage()));
        mailLogService.saveMailLog(emailLog);
        sentEmailContentList.add(emailContent);
      } else {
        emailContent.setErrorMessage("Error Message: ".concat(e.getMessage()));
        emailContentService.saveEmailContent(emailContent);
      }
    }
  }

  private Email buildMail(EmailContent emailContent) {
    Email email = new Email();
    email.setContent(emailContent.getContentMssg());
    email.setFrom(emailContent.getFromEmail());
    email.setTo(buildStringArray(emailContent.getToEmail()));
    email.setSubject(emailContent.getSubject());
    email.setAttachments(emailContent.getAttachments());
    if (emailContent.getCcEmails() != null && !emailContent.getCcEmails().isEmpty()) {
      email.setCc(buildStringArray(emailContent.getCcEmails()));
    }
    return email;
  }

  private String[] buildStringArray(String value) {
    if (value != null) {
      return value.split(" ");
    } else {
      return new String[0];
    }
  }

  public String generateMailHtml(String body, String title) {
    final String templateFileName = "email"; // Name of the HTML template file without extension

    String output =
        this.templateEngine.process(templateFileName, new Context(Locale.getDefault(), null));

    output = output.replace("{BODY}", body);
    output = output.replace("{TITLE}", title);
    return output;
  }

  public String getCCEmails(Email email) {
    StringBuilder emails = new StringBuilder();
    if (email != null && email.getCc() != null) {
      for (String em : email.getCc()) {
        emails.append(em).append(" ");
      }
    }
    return emails.toString();
  }

  public String getToEmails(Email email) {
    StringBuilder emails = new StringBuilder();
    if (email != null && email.getTo() != null) {
      for (String em : email.getTo()) {
        emails.append(em).append(" ");
      }
    }
    return emails.toString();
  }
}
