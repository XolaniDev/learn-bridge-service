package co.za.learn.bridge.model.entity;

import co.za.learn.bridge.model.dto.Attachment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@Document(collection = "email_content")
public class EmailContent implements Serializable {

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
  private Integer retryCount;
  @NotBlank
  @Size(max = 5000)
  private String errorMessage;
  private Date runDate;
  private Boolean deleted;
  private List<Attachment> attachments;


}
