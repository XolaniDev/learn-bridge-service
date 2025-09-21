package co.za.learn.bridge.model.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateLoginDetailsRequest {

  @NotBlank private String userId;
  @NotBlank private String currentPassword;
  @NotBlank private String newPassword;

}
