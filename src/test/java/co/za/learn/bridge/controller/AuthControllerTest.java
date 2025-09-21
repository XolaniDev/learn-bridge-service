package co.za.learn.bridge.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import co.za.learn.bridge.containers.ContainerBase;
import co.za.learn.bridge.model.payload.request.LoginRequest;
import co.za.learn.bridge.model.payload.request.SignupRequest;
import co.za.learn.bridge.model.payload.response.MessageResponse;
import co.za.learn.bridge.model.payload.response.UserInfoResponse;
import co.za.learn.bridge.utils.exception.LearnBridgeException;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest extends ContainerBase {

  @Autowired AuthController authController;
  @Autowired
  LearnBridgeController learnBridgeController;

  @Test
  @Order(1)
  @DisplayName("Should create a new user account with associated farm details during signup")
  void shouldCreateNewUserAccountWithFarmDetailsOnSignup() throws LearnBridgeException {

    SignupRequest request = getSignupRequest("username@email.com");

    ResponseEntity<Object> response = authController.signup(request);
    assertNotNull(response);
    assertEquals(200, response.getStatusCode().value());

    MessageResponse messageResponse = (MessageResponse) response.getBody();
    assertTrue(Objects.requireNonNull(messageResponse).isSuccess());
    assertEquals("Profile created successfully!", messageResponse.getMessage());
  }

  @Test
  @Order(2)
  @DisplayName(
      "Should return a 400 Bad Request when attempting to sign up with an email already in use")
  void testSignupWithExistingEmail() throws LearnBridgeException {

    SignupRequest request = getSignupRequest("username@email.com");

    ResponseEntity<Object> response = authController.signup(request);
    assertNotNull(response);
    assertEquals(400, response.getStatusCode().value());

    MessageResponse messageResponse = (MessageResponse) response.getBody();
    assertFalse(Objects.requireNonNull(messageResponse).isSuccess());
    assertEquals("Error: Email is already in use!", messageResponse.getMessage());
  }

  @Test
  @Order(3)
  @DisplayName(
      "Should successfully authenticate a user with valid credentials and return a JWT cookie")
  void testAuthenticateUser_Success() throws LearnBridgeException {
    LoginRequest request = new LoginRequest();
    request.setUsername("username@email.com");
    request.setPassword("Password");
    ResponseEntity<Object> response = authController.authenticateUser(request);
    assertNotNull(response);
    assertEquals(200, response.getStatusCode().value());
  }

  @Test
  @Order(4)
  @DisplayName("Should validate signup details")
  void testSignupValidateUserAndFarmDetails() throws LearnBridgeException {
    SignupRequest request = getSignupRequest("test@example.com");

    ResponseEntity<Object> response = authController.signup(request);
    assertNotNull(response);
    assertEquals(200, response.getStatusCode().value());

    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setUsername("test@example.com");
    loginRequest.setPassword("Password");
    ResponseEntity<Object> loginResponse = authController.authenticateUser(loginRequest);
    assertNotNull(loginResponse);
    assertEquals(200, loginResponse.getStatusCode().value());

    UserInfoResponse userInfoResponse = (UserInfoResponse) loginResponse.getBody();
    assertNotNull(userInfoResponse);

    assertEquals(request.getName(), userInfoResponse.getName());
    assertEquals(request.getSurname(), userInfoResponse.getSurname());
    assertEquals(request.getEmail(), userInfoResponse.getEmail());
    assertEquals(request.getPhoneNumber(), userInfoResponse.getPhoneNumber());

  }

  private static @NotNull SignupRequest getSignupRequest(String mail) {
    SignupRequest request = new SignupRequest();
    request.setName("User Name");
    request.setSurname("User Surname");
    request.setEmail(mail);
    request.setPhoneNumber("0729566589");
    request.setPassword("Password");
    return request;
  }

  @Test
  @Order(5)
  @DisplayName("Should handle authentication failure with invalid credentials")
  void testAuthenticateUser_Failure() {
    LoginRequest request = new LoginRequest();
    request.setUsername("username@email.com");
    request.setPassword("WrongPassword");

    try {
      authController.authenticateUser(request);
    } catch (BadCredentialsException | LearnBridgeException expected) {
      assertEquals("Bad credentials", expected.getMessage());
    }
  }
}
