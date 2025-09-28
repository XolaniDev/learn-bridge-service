package co.za.learn.bridge.model.entity.lookups;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Date;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "app_config")
public class AppConfig {
  @Id
  private String id;
  @NotBlank
  @Size(max = 50)
  private String code;
  @NotBlank
  @Size(max = 250)
  private String description;
  @NotBlank
  private Date createDate;
  @NotBlank
  private Date lastUpdateDate;
  private String lastUpdateUserId;
}
