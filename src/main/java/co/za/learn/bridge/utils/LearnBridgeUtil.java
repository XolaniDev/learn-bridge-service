package co.za.learn.bridge.utils;

import co.za.learn.bridge.model.dto.ERole;
import co.za.learn.bridge.model.entity.Role;
import co.za.learn.bridge.model.entity.User;
import co.za.learn.bridge.model.payload.response.UserInfoResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LearnBridgeUtil {
  static Random random = new Random();

  public static String generatePassword() {
    String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String lower = "abcdefghijklmnopqrstuvwxyz";
    String digits = "0123456789";
    String allChars = upper + lower + digits;
    int length = 6;

    StringBuilder password = new StringBuilder(length);
    password.append(upper.charAt(random.nextInt(upper.length())));
    password.append(digits.charAt(random.nextInt(digits.length())));
    for (int i = 2; i < length; i++) {
      password.append(allChars.charAt(random.nextInt(allChars.length())));
    }

    return password.toString();
  }

  public static UserInfoResponse getUserInfoResponse(User user) {
    return UserInfoResponse.builder()
        .id(user.getId())
        .name(user.getName())
        .surname(user.getSurname())
        .email(user.getEmail())
        .phoneNumber(user.getPhoneNumber())
        .province(user.getProvince())
        .grade(user.getGrade())
        .interests(user.getInterests())
        .subjects(user.getSubjects())
        .financialBackground(user.getFinancialBackground())
        .createdDate(user.getCreatedDate())
        .learnerNumber(user.getLearnerNumber())
        .roles(user.getRoles().stream().map(Role::getName).map(Enum::name).toList())
        .changePassword(user.isChangePassword())
        .roleFriendlyNames(
            user.getRoles().stream().map(Role::getName).map(ERole::getValue).toList())
        .build();
  }

  /**
   * Generates a unique learner number in the following format:
   *
   * <pre>
   * {First letter of first name}{First letter of surname}{Creation date as ddMMyy}
   * {Last two digits of cell number}{Random two-digit number between 10-99}
   * </pre>
   *
   * <p>Example: If the user's name is Christoph Sibiya, creation date is 27 Sep 2025, and cell
   * number is 0823456789, a possible output is: <strong>CS2709258917</strong>.
   *
   * @param firstName The first name of the learner. Must not be null or empty.
   * @param surname The surname of the learner. Must not be null or empty.
   * @param cellNumber The learner's cell number. Must have at least 2 digits.
   * @return A unique learner number as a String.
   * @throws IllegalArgumentException if any input is invalid (null, empty, or cell number too
   *     short)
   */
  public static String generateLearnerNumber(
      String firstName, String surname, String cellNumber, Date date) {
    if (firstName == null
        || firstName.isEmpty()
        || surname == null
        || surname.isEmpty()
        || cellNumber == null
        || cellNumber.length() < 2) {
      throw new IllegalArgumentException("Invalid input data");
    }

    // Get initials
    String initials =
        firstName.substring(0, 1).toUpperCase() + surname.substring(0, 1).toUpperCase();

    // Get creation date in ddMMyy format
    String datePart = new SimpleDateFormat("ddMMyy").format(date);

    // Get last 2 digits of cell number
    String cellPart = cellNumber.substring(cellNumber.length() - 2);

    // Generate random 2-digit number
    Random random = new Random();
    int randomNum = 10 + random.nextInt(90); // 10-99

    // Combine all parts
    return initials + datePart + cellPart + randomNum;
  }
}
