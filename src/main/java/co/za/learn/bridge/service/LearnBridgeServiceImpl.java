package co.za.learn.bridge.service;

import co.za.learn.bridge.model.entity.Recommendations;
import co.za.learn.bridge.model.entity.User;
import co.za.learn.bridge.model.payload.request.LoginRequest;
import co.za.learn.bridge.model.payload.request.UpdateLoginDetailsRequest;
import co.za.learn.bridge.model.payload.request.UpdateProfileSetupRequest;
import co.za.learn.bridge.model.payload.request.UpdateUserRequest;
import co.za.learn.bridge.model.payload.response.*;
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
  private final AsyncService asyncService;
  private final OpenAiService openAiService;

  @Override
  public ResponseEntity<Object> updateUser(UpdateUserRequest request) {

    User user;
    try {

      Optional<User> userByEmail = userRepository.findByEmail(request.getEmail());

      if (userByEmail.isPresent()
          && !Objects.equals(userByEmail.get().getId(), request.getUserId())) {
        return ResponseEntity.badRequest()
            .body(new SignupResponse(false, "Error: Email is already in use!", null));
      }
      user = userRepository.save(getUser(request));
      logger.info("User update successful");
      return ResponseEntity.ok(LearnBridgeUtil.getUserInfoResponse(user));
    } catch (LearnBridgeException e) {
      logger.error("Update user Error: ", e);
      return ResponseEntity.badRequest().body(new MessageResponse(false, e.getMessage()));
    }
  }

  @Override
  public ResponseEntity<Object> updateProfileSetup(UpdateProfileSetupRequest request) {

    Optional<User> optionalUser = getUserOptional(request.getUserId());
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      user.setInterests(request.getInterests());
      user.setProvince(request.getProvince());
      user.setGrade(request.getGrade());
      user.setSubjects(request.getSubjects());
      user.setFinancialBackground(request.getFinancialBackground());
      userRepository.save(user);
      openAiService.generateRecommendations(user.getId());
      logger.info("User update successful");
      return ResponseEntity.ok(LearnBridgeUtil.getUserInfoResponse(user));
    } else {
      return ResponseEntity.badRequest().body(new MessageResponse(false, "User not found"));
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
          user.setChangePassword(false);
          userRepository.save(user);
          asyncService.notifyUserPasswordChange(user, request.getNewPassword());
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

  @Override
  public ResponseEntity<Object> findUserById(String userId) {

    Optional<User> optionalUser = getUserOptional(userId);
    User user;
    if (optionalUser.isPresent()) {
      user = optionalUser.get();
      return ResponseEntity.ok(LearnBridgeUtil.getUserInfoResponse(user));
    } else {
      logger.info("Invalid user ID: {}", userId);
      return ResponseEntity.badRequest().body(new MessageResponse(false, "Invalid user ID"));
    }
  }

  @Override
  public ResponseEntity<Object> getFundingData(String userId) {

    Optional<User> optionalUser = getUserOptional(userId);
    if (optionalUser.isPresent()) {
      Recommendations recommendation = openAiService.getUserRecommendations(userId);
      return ResponseEntity.ok(
          new FundingResponse(
              openAiService.findFundingDetails(recommendation.getRecommendedCourses(), userId)));
    } else {
      logger.info("Invalid user ID: {}", userId);
      return ResponseEntity.badRequest().body(new MessageResponse(false, "Invalid user ID"));
    }
  }

  @Override
  public ResponseEntity<Object> getDashboardData(String userId) {

    Optional<User> optionalUser = getUserOptional(userId);
    User user;
    if (optionalUser.isPresent()) {
      user = optionalUser.get();

      Recommendations recommendation = openAiService.getUserRecommendations(userId);

      return ResponseEntity.ok(
          new DashboardResponse(
              Optional.ofNullable(user.getSubjects()).map(List::size).orElse(0),
              Optional.ofNullable(user.getInterests()).map(List::size).orElse(0),
              Optional.of(recommendation.getRecommendedCourses()).map(List::size).orElse(0),
              Optional.of(recommendation.getRecommendedCourses()).orElseGet(Collections::emptyList),
              Optional.of(recommendation.getJobTrends()).orElseGet(Collections::emptyList),
              Optional.of(recommendation.getFundingOpportunities())
                  .orElseGet(Collections::emptyList),
              recommendation.getCareerGrowthTips()));

    } else {
      logger.info("Invalid user ID: {}", userId);
      return ResponseEntity.badRequest().body(new MessageResponse(false, "Invalid user ID"));
    }
  }

  @Override
  public ResponseEntity<Object> getJobMarket(String userId) {
    Recommendations recommendation = openAiService.getUserRecommendations(userId);
    return ResponseEntity.ok(recommendation.getJobMarket());
  }

  private User getUser(UpdateUserRequest request) throws LearnBridgeException {
    Optional<User> optionalUser = getUserOptional(request.getUserId());
    User user;
    if (optionalUser.isPresent()) {
      user = optionalUser.get();
      user.setName(request.getName());
      user.setSurname(request.getSurname());
      user.setEmail(request.getEmail());
      user.setPhoneNumber(request.getPhoneNumber());
      if (user.getLearnerNumber() == null) {
        user.setLearnerNumber(
            LearnBridgeUtil.generateLearnerNumber(
                request.getName(),
                request.getSurname(),
                request.getPhoneNumber(),
                user.getCreatedDate()));
      }
    } else {
      user = new User();
      user.setName(request.getName());
      user.setSurname(request.getSurname());
      user.setEmail(request.getEmail());
      user.setPhoneNumber(request.getPhoneNumber());
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
