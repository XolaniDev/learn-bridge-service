package co.za.learn.bridge.model.entity;

import co.za.learn.bridge.model.dto.ERole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@Document(collection = "roles")
public class Role {

  @Id private String id;

  private ERole name;
}
