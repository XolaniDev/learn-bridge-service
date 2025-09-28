package co.za.learn.bridge.controller;

import co.za.learn.bridge.model.payload.request.LoginRequest;
import co.za.learn.bridge.model.payload.request.ProfileSetupRequest;
import co.za.learn.bridge.model.payload.request.SignupRequest;
import co.za.learn.bridge.model.payload.response.ForgotPasswordRequest;
import co.za.learn.bridge.service.AuthControllerService;
import co.za.learn.bridge.utils.exception.LearnBridgeException;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

  private static final Logger logger = LogManager.getLogger(AuthController.class);
  private AuthControllerService authControllerService;

  @PostMapping("/signin")
  public ResponseEntity<Object> authenticateUser(@Valid @RequestBody LoginRequest loginRequest)
      throws LearnBridgeException {
    logger.info("Authenticating user: {}", loginRequest.getUsername());
    return authControllerService.authenticateUser(loginRequest);
  }

  @PostMapping("/signup")
  public ResponseEntity<Object> signup(@Valid @RequestBody SignupRequest request)
      throws LearnBridgeException {
    logger.info(
        "Registering user: [Name: {}, Surname: {}, Email: {}, Phone Number: {}]",
        request.getName(),
        request.getSurname(),
        request.getEmail(),
        request.getPhoneNumber());
    return authControllerService.signup(request);
  }

  @PostMapping("/profile-setup")
  public ResponseEntity<Object> profileSetup(@Valid @RequestBody ProfileSetupRequest request)
          throws LearnBridgeException {
    logger.info(
            "Registering user: [Province: {}, Grade: {},  interests: {}, subjects: {}, financialBackground: {}   ]",
            request.getProvince(),
            request.getGrade(),
            request.getInterests(),
            request.getSubjects(),
            request.getFinancialBackground());
    return authControllerService.profileSetup(request);
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<Object> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    return authControllerService.forgotPassword(request);
  }

}
