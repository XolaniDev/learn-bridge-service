package co.za.learn.bridge.model.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileSetupRequest {

  @NotBlank
  private String userId;

  @NotBlank
  @Size(max = 50)
  private String province;

  @NotBlank
  @Size(max = 50)
  private String grade;

  private List<String> interests;

  private List<String> subjects;

  private String financialBackground;

}
