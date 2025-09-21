package co.za.learn.bridge.utils;

import java.util.Random;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationUtil {
  static Random random = new Random();

  /**
   * Validates whether a given password meets the specified criteria. The password must: - Be at
   * least 8 characters long - Contain at least one uppercase letter - Contain at least one
   * lowercase letter - Contain at least one number - Contain at least one special character
   * (!@#$%^&*())
   *
   * @param password The password string to be validated
   * @return true if the password meets all criteria, false otherwise
   */
  public static boolean isValidPassword(String password) {
    if (password == null || password.length() < 8) {
      return false;
    }

    // Regular expressions for each criteria
    String uppercaseRegex = ".*[A-Z].*";
    String lowercaseRegex = ".*[a-z].*";
    String numberRegex = ".*[0-9].*";
    String specialCharRegex = ".*[!@#$%^&*()].*"; // You can customize the special characters here

    // Check if password matches all criteria
    return password.matches(uppercaseRegex)
        && password.matches(lowercaseRegex)
        && password.matches(numberRegex)
        && password.matches(specialCharRegex);
  }
}
