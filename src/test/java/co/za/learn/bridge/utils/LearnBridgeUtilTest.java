package co.za.learn.bridge.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LearnBridgeUtilTest {

  @Test
  void generatePassword() {
    for (int i = 0; i < 10; i++) {
      String password = LearnBridgeUtil.generatePassword();
      Assertions.assertTrue(password.matches("[a-zA-Z0-9]{10}"));
    }
  }
}
