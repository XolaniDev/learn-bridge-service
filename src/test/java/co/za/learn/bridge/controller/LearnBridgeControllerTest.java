package co.za.learn.bridge.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.za.learn.bridge.containers.ContainerBase;
import co.za.learn.bridge.model.payload.request.LoginRequest;
import co.za.learn.bridge.model.payload.request.SignupRequest;
import co.za.learn.bridge.model.payload.request.UpdateLoginDetailsRequest;
import co.za.learn.bridge.model.payload.request.UpdateUserRequest;
import co.za.learn.bridge.model.payload.response.MessageResponse;
import co.za.learn.bridge.model.payload.response.UserInfoResponse;
import co.za.learn.bridge.repository.UserRepository;
import co.za.learn.bridge.utils.exception.LearnBridgeException;
import java.util.*;
import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LearnBridgeControllerTest extends ContainerBase {

  @Autowired AuthController authController;
  @Autowired
  LearnBridgeController learnBridgeController;
  @Autowired UserRepository userRepository;

  @BeforeEach
  void setUp() {
    getSignupRequests()
        .forEach(
            request -> {
              try {
                authController.signup(request);
              } catch (LearnBridgeException e) {
                throw new RuntimeException(e);
              }
            });
  }

  @Test
  @Order(1)
  @DisplayName(
      "updateUser - Should successfully update user information when valid request is provided")
  void testUpdateUser_Success() throws LearnBridgeException {
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setUsername("useremai@gmail.com");
    loginRequest.setPassword("Password");
    ResponseEntity<Object> loginResponse = authController.authenticateUser(loginRequest);
    assertNotNull(loginResponse);
    assertEquals(200, loginResponse.getStatusCode().value());
    UserInfoResponse userInfoResponse = (UserInfoResponse) loginResponse.getBody();
    assertNotNull(userInfoResponse);

    UpdateUserRequest updateRequest = new UpdateUserRequest();
    updateRequest.setId(userInfoResponse.getId());
    updateRequest.setName("UpdatedName");
    updateRequest.setSurname("UpdatedSurname");
    updateRequest.setEmail("UpdatedEmail");
    updateRequest.setPhoneNumber("UpdatedPhoneNumber");

    ResponseEntity<Object> response = learnBridgeController.updateUser(updateRequest);
    UserInfoResponse newUserInfo = (UserInfoResponse) response.getBody();

    assertNotNull(newUserInfo);
    assertEquals(updateRequest.getName(), newUserInfo.getName());
    assertEquals(updateRequest.getSurname(), newUserInfo.getSurname());
    assertEquals(updateRequest.getEmail(), newUserInfo.getEmail());
    assertEquals(updateRequest.getPhoneNumber(), newUserInfo.getPhoneNumber());
    assertEquals(userInfoResponse.getId(), newUserInfo.getId());
  }

  @Test
  @Order(2)
  @DisplayName(
      "updateUser - Should return Farm Id cannot be null error response when new user is created without farm ID")
  void testUpdateUserWithInvalidRequest() {

    UpdateUserRequest updateRequest = new UpdateUserRequest();

    updateRequest.setName("NoFarmIdNewName");
    updateRequest.setSurname("NoFarmIdNewSurname");
    updateRequest.setEmail("NoFarmIdNewEmail");
    updateRequest.setPhoneNumber("NoFarmIdNewPhoneNumber");

    ResponseEntity<Object> response = learnBridgeController.updateUser(updateRequest);
    assertNotNull(response);
    assertEquals(400, response.getStatusCode().value());
    MessageResponse messageResponse = (MessageResponse) response.getBody();
    assertFalse(Objects.requireNonNull(messageResponse).isSuccess());
    assertEquals("Farm Id cannot be null", messageResponse.getMessage());
  }

  @Test
  @Order(3)
  @DisplayName(
      "updateUser - Should return Invalid Farm Id error response when new user is created with invalid farm ID")
  void testUpdateUserWithInvalidFarmIDRequest() {

    UpdateUserRequest updateRequest = new UpdateUserRequest();

    updateRequest.setFarmId("InvalidFarmID");
    updateRequest.setName("InvFarmIdNewName");
    updateRequest.setSurname("InvFarmIdNewSurname");
    updateRequest.setEmail("InvFarmIdNewEmail");
    updateRequest.setPhoneNumber("InvFarmIdNewPhoneNumber");

    ResponseEntity<Object> response = learnBridgeController.updateUser(updateRequest);
    assertNotNull(response);
    assertEquals(400, response.getStatusCode().value());
    MessageResponse messageResponse = (MessageResponse) response.getBody();
    assertFalse(Objects.requireNonNull(messageResponse).isSuccess());
    assertEquals("Invalid Farm Id", messageResponse.getMessage());
  }

  @Test
  @Order(4)
  @DisplayName(
      "updateUser - Should successfully create a new user and link the user with associated farm id")
  void testCreateUserWithValidRequest() {

    UpdateUserRequest updateRequest = new UpdateUserRequest();
    UserInfoResponse userInfoResponse = getLoggedUser();

    updateRequest.setName("NewFarmUserName");
    updateRequest.setSurname("NewFarmUserSurname");
    updateRequest.setEmail("NewEmail");
    updateRequest.setPhoneNumber("NewPhoneNumber");

    ResponseEntity<Object> response = learnBridgeController.updateUser(updateRequest);
    assertNotNull(response);
    UserInfoResponse updatedUserInfo = (UserInfoResponse) response.getBody();
    assertEquals(200, response.getStatusCode().value());
    assertEquals("NewFarmUserName", Objects.requireNonNull(updatedUserInfo).getName());
  }





  @Test
  @Order(36)
  @DisplayName("updateLoginDetails - Should successfully update password when all inputs are valid")
  void shouldSuccessfullyUpdatePasswordWhenAllInputsAreValid() throws LearnBridgeException {

    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setUsername("chrissibiya@gmail.com");
    loginRequest.setPassword("Password");
    ResponseEntity<Object> loginResponse = authController.authenticateUser(loginRequest);
    UserInfoResponse loggedUser = (UserInfoResponse) loginResponse.getBody();

    UpdateLoginDetailsRequest request = new UpdateLoginDetailsRequest();
    assert loggedUser != null;
    request.setUserId(loggedUser.getId());
    request.setCurrentPassword("Password");
    request.setNewPassword("NewValidPassword123!");

    ResponseEntity<Object> response = learnBridgeController.updateLoginDetails(request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    MessageResponse messageResponse = (MessageResponse) response.getBody();
    assertNotNull(messageResponse);
    assertTrue(messageResponse.isSuccess());
    assertEquals("Password updated successfully", messageResponse.getMessage());

    loginRequest.setUsername("chrissibiya@gmail.com");
    loginRequest.setPassword("NewValidPassword123!");
    loginResponse = authController.authenticateUser(loginRequest);
    assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
  }

  @Test
  @Order(37)
  @DisplayName("updateLoginDetails - Should return bad request when user ID is invalid")
  void shouldReturnBadRequestWhenUserIdIsInvalid() throws LearnBridgeException {

    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setUsername("chrissibiya@gmail.com");
    loginRequest.setPassword("NewValidPassword123!");
    ResponseEntity<Object> loginResponse = authController.authenticateUser(loginRequest);
    UserInfoResponse loggedUser = (UserInfoResponse) loginResponse.getBody();

    UpdateLoginDetailsRequest request = new UpdateLoginDetailsRequest();
    assert loggedUser != null;
    request.setUserId(loggedUser.getId());
    request.setCurrentPassword("InvalidPassword");
    request.setNewPassword("NewPassword");

    ResponseEntity<Object> response = learnBridgeController.updateLoginDetails(request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertInstanceOf(MessageResponse.class, response.getBody());
    MessageResponse messageResponse = (MessageResponse) response.getBody();
    assertFalse(messageResponse.isSuccess());
    assertEquals(
        "We are unable to update your password, please verify that your current password is correct and try again",
        messageResponse.getMessage());
  }

  @Test
  @Order(38)
  @DisplayName("updateLoginDetails - Should return bad request when new password is invalid")
  void shouldReturnBadRequestWhenNewPasswordIsInvalid() throws LearnBridgeException {

    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setUsername("chrissibiya@gmail.com");
    loginRequest.setPassword("NewValidPassword123!");
    ResponseEntity<Object> loginResponse = authController.authenticateUser(loginRequest);
    UserInfoResponse loggedUser = (UserInfoResponse) loginResponse.getBody();

    UpdateLoginDetailsRequest request = new UpdateLoginDetailsRequest();
    assert loggedUser != null;
    request.setUserId(loggedUser.getId());
    request.setCurrentPassword("NewValidPassword123!");
    request.setNewPassword("InValidaNewPassword");

    ResponseEntity<Object> response = learnBridgeController.updateLoginDetails(request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertInstanceOf(MessageResponse.class, response.getBody());
    MessageResponse messageResponse = (MessageResponse) response.getBody();
    assertFalse(messageResponse.isSuccess());
    assertEquals(
        "Invalid password, please provide a strong password", messageResponse.getMessage());
  }

  @Test
  @Order(39)
  @DisplayName("updateLoginDetails - Should return bad request when current password is incorrect")
  void shouldReturnBadRequestWhenCurrentPasswordIsIncorrect() {

    UpdateLoginDetailsRequest request = new UpdateLoginDetailsRequest();
    request.setUserId("invalidUserId");

    ResponseEntity<Object> response = learnBridgeController.updateLoginDetails(request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertInstanceOf(MessageResponse.class, response.getBody());
    MessageResponse messageResponse = (MessageResponse) response.getBody();
    assertFalse(messageResponse.isSuccess());
    assertEquals("Invalid user ID", messageResponse.getMessage());
  }





  private static @NotNull List<SignupRequest> getSignupRequests() {
    SignupRequest request1 = new SignupRequest();
    request1.setName("Init-Name");
    request1.setSurname("Init-Surname");
    request1.setEmail("useremai@gmail.com");
    request1.setPhoneNumber("0729566589");
    request1.setPassword("Password");

    SignupRequest request2 = new SignupRequest();
    request2.setName("Init-UserName");
    request2.setSurname("Init-UserSurname");
    request2.setEmail("useremai2@gmail.com");
    request2.setPhoneNumber("0729566589");
    request2.setPassword("Password");

    SignupRequest request3 = new SignupRequest();
    request3.setName("Chris");
    request3.setSurname("Sibiya");
    request3.setEmail("chrissibiya@gmail.com");
    request3.setPhoneNumber("0729566589");
    request3.setPassword("Password");

    List<SignupRequest> signupRequests = new ArrayList<>();
    signupRequests.add(request1);
    signupRequests.add(request2);
    signupRequests.add(request3);

    return signupRequests;
  }

  private @Nullable UserInfoResponse getLoggedUser() {
    try {
      LoginRequest loginRequest = new LoginRequest();
      loginRequest.setUsername("useremai@gmail.com");
      loginRequest.setPassword("Password");
      ResponseEntity<Object> loginResponse = authController.authenticateUser(loginRequest);
      return (UserInfoResponse) loginResponse.getBody();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
