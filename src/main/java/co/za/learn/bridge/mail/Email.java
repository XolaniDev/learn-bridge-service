package co.za.learn.bridge.mail;

import co.za.learn.bridge.model.dto.Attachment;
import java.util.List;
import lombok.Data;

@Data
public class Email {
  private Integer id;
  private String from;
  private String[] to;
  private String[] cc;
  private String subject;
  private String content;
  private List<Attachment> attachments;

  public Email() {
    content = "text/plain";
  }
}