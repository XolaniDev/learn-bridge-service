package co.za.learn.bridge.model.payload.response;

import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserInfoResponse {

  private String id;
  private String name;
  private String surname;
  private String phoneNumber;
  private Date createdDate;
  private String email;
  private String province;
  private String grade;
  private List<String> interests;
  private List<String> subjects;
  private String financialBackground;
  private List<String> roles;
  private List<String> roleFriendlyNames;
  private String learnerNumber;
  private boolean changePassword;

}
