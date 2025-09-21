package co.za.learn.bridge.model.entity;


import java.util.Date;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@Document(collection = "mail_log")
public class EmailLog {

  @Id
  private String id;
  @NotBlank
  private Date createDate;
  @NotBlank
  private Date lastUpdateDate;
  @NotBlank
  @Size(max = 250)
  private String fromEmail;
  @NotBlank
  @Size(max = 250)
  private String toEmail;

  @Size(max = 250)
  private String ccEmails;
  @NotBlank
  @Size(max = 250)
  private String subject;
  @NotBlank
  @Size(max = 5000)
  private String contentMssg;
  private Boolean error;
  @Size(max = 5000)
  private String errorMessage;
  
  private String lastUpdateUserId;

  public EmailLog(
      String fromEmail,
      String toEmail,
      String ccEmails,
      String subject,
      String contentMssg,
      Boolean error,
      String errorMessage) {
    super();
    this.fromEmail = fromEmail;
    this.toEmail = toEmail;
    this.ccEmails = ccEmails;
    this.subject = subject;
    this.contentMssg = contentMssg;
    this.error = error;
    this.errorMessage = errorMessage;
  }
}
