package co.za.learn.bridge.service;

import co.za.learn.bridge.mail.EmailSender;
import co.za.learn.bridge.model.dto.ERole;
import co.za.learn.bridge.model.entity.Role;
import co.za.learn.bridge.model.entity.User;
import co.za.learn.bridge.model.payload.request.LoginRequest;
import co.za.learn.bridge.model.payload.request.UpdateLoginDetailsRequest;
import co.za.learn.bridge.model.payload.request.UpdateUserRequest;
import co.za.learn.bridge.model.payload.response.MessageResponse;
import co.za.learn.bridge.model.payload.response.UserInfoResponse;
import co.za.learn.bridge.repository.UserRepository;
import co.za.learn.bridge.utils.LearnBridgeUtil;
import co.za.learn.bridge.utils.ValidationUtil;
import co.za.learn.bridge.utils.exception.LearnBridgeException;
import java.util.*;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LearnBridgeServiceImpl implements LearnBridgeService {
  private static final Logger logger = LogManager.getLogger(LearnBridgeServiceImpl.class);
  private final UserRepository userRepository;
  private final PasswordEncoder encoder;
  private final AuthControllerService authControllerService;
  private final EmailSender emailSender;

  @Override
  public ResponseEntity<Object> updateUser(UpdateUserRequest request) {

    User user;
    try {
      user = userRepository.save(getUser(request));
      UserInfoResponse userInfoResponse =
          UserInfoResponse.builder()
              .id(user.getId())
              .name(user.getName())
              .surname(user.getSurname())
              .email(user.getEmail())
              .phoneNumber(user.getPhoneNumber())
              .province(request.getProvince())
              .grade(request.getGrade())
              .interests(request.getInterests())
              .subjects(request.getSubjects())
              .financialBackground(request.getFinancialBackground())
              .createdDate(user.getCreatedDate())
              .roles(user.getRoles().stream().map(Role::getName).map(Enum::name).toList())
              .roleFriendlyNames(
                  user.getRoles().stream().map(Role::getName).map(ERole::getValue).toList())
              .build();
      logger.info("User update successful");
      return ResponseEntity.ok(userInfoResponse);
    } catch (LearnBridgeException e) {
      logger.error("Update user Error: ", e);
      return ResponseEntity.badRequest().body(new MessageResponse(false, e.getMessage()));
    }
  }

  @Override
  public ResponseEntity<Object> updateLoginDetails(UpdateLoginDetailsRequest request) {
    try {

      Optional<User> optionalUser = getUserOptional(request.getUserId());
      User user;
      if (optionalUser.isPresent()) {
        user = optionalUser.get();
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(user.getEmail());
        loginRequest.setPassword(request.getCurrentPassword());

        // Will throw exception if login details are invalid
        authControllerService.authenticateUser(loginRequest);

        boolean isPasswordValidation = ValidationUtil.isValidPassword(request.getNewPassword());

        if (isPasswordValidation) {
          user.setPassword(encoder.encode(request.getNewPassword()));
          userRepository.save(user);
          return ResponseEntity.ok(new MessageResponse(true, "Password updated successfully"));
        } else {
          logger.info("Invalid password, please provide a strong password");
          return ResponseEntity.badRequest()
              .body(
                  new MessageResponse(false, "Invalid password, please provide a strong password"));
        }

      } else {
        logger.info("Invalid user ID: {}", request.getUserId());
        return ResponseEntity.badRequest().body(new MessageResponse(false, "Invalid user ID"));
      }

    } catch (LearnBridgeException e) {
      logger.error("Error while updating login details: ", e);
      return ResponseEntity.badRequest()
          .body(
              new MessageResponse(
                  false,
                  "We are unable to update your password, please verify that your current password is correct and try again"));
    }
  }

  private User getUser(UpdateUserRequest request) throws LearnBridgeException {
    Optional<User> optionalUser = getUserOptional(request.getId());
    User user;
    if (optionalUser.isPresent()) {
      user = optionalUser.get();
      user.setName(request.getName());
      user.setSurname(request.getSurname());
      user.setEmail(request.getEmail());
      user.setProvince(request.getProvince());
      user.setGrade(request.getGrade());
      user.setInterests(request.getInterests());
      user.setSubjects(request.getSubjects());
      user.setFinancialBackground(request.getFinancialBackground());
      user.setPhoneNumber(request.getPhoneNumber());
    } else {
      user = new User();
      user.setName(request.getName());
      user.setSurname(request.getSurname());
      user.setEmail(request.getEmail());
      user.setPhoneNumber(request.getPhoneNumber());
      user.setProvince(request.getProvince());
      user.setGrade(request.getGrade());
      user.setInterests(request.getInterests());
      user.setSubjects(request.getSubjects());
      user.setFinancialBackground(request.getFinancialBackground());
      user.setPassword(encoder.encode(LearnBridgeUtil.generatePassword()));
    }
    return user;
  }

  private Optional<User> getUserOptional(String id) {
    if (id != null) {
      return userRepository.findById(id);
    } else {
      return Optional.empty();
    }
  }
}
