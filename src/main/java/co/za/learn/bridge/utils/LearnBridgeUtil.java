package co.za.learn.bridge.utils;

import java.util.Random;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LearnBridgeUtil {
  static Random random = new Random();

  public static String generatePassword() {
    String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    int length = 10;
    StringBuilder password = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      int index = random.nextInt(chars.length());
      password.append(chars.charAt(index));
    }

    return password.toString();
  }


}
