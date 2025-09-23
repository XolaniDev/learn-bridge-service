package co.za.learn.bridge.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignupResponse {
  private boolean success;
  private String message;
  private String userId;
}
