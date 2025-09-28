package co.za.learn.bridge.service;

import co.za.learn.bridge.excption.NotFoundException;
import co.za.learn.bridge.model.dto.ERole;
import co.za.learn.bridge.model.entity.Role;
import co.za.learn.bridge.model.entity.User;
import co.za.learn.bridge.model.payload.request.LoginRequest;
import co.za.learn.bridge.model.payload.request.ProfileSetupRequest;
import co.za.learn.bridge.model.payload.request.SignupRequest;
import co.za.learn.bridge.model.payload.response.ForgotPasswordRequest;
import co.za.learn.bridge.model.payload.response.MessageResponse;
import co.za.learn.bridge.model.payload.response.SignupResponse;
import co.za.learn.bridge.model.payload.response.UserInfoResponse;
import co.za.learn.bridge.repository.RoleRepository;
import co.za.learn.bridge.repository.UserRepository;
import co.za.learn.bridge.security.jwt.JwtUtils;
import co.za.learn.bridge.security.services.UserDetailsImpl;
import co.za.learn.bridge.utils.ConstantUtil;
import co.za.learn.bridge.utils.LearnBridgeUtil;
import co.za.learn.bridge.utils.exception.LearnBridgeException;
import java.util.*;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthControllerServiceImpl implements AuthControllerService {
  private static final Logger logger = LogManager.getLogger(AuthControllerServiceImpl.class);
  AuthenticationManager authenticationManager;
  UserRepository userRepository;
  RoleRepository roleRepository;
  PasswordEncoder encoder;
  JwtUtils jwtUtils;
  AsyncService asyncService;

  @Override
  public ResponseEntity<Object> authenticateUser(LoginRequest loginRequest)
      throws LearnBridgeException {
    try {
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  loginRequest.getUsername(), loginRequest.getPassword()));

      SecurityContextHolder.getContext().setAuthentication(authentication);

      UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

      ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

      List<String> roles =
          userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

      UserInfoResponse response =
          UserInfoResponse.builder()
              .id(userDetails.getId())
              .name(userDetails.getName())
              .surname(userDetails.getSurname())
              .phoneNumber(userDetails.getPhoneNumber())
              .createdDate(userDetails.getCreatedDate())
              .email(userDetails.getEmail())
              .roles(roles)
              .roleFriendlyNames(getRoleFriendlyNames(roles))
              .province(userDetails.getProvince())
              .grade(userDetails.getGrade())
              .interests(userDetails.getInterests())
              .subjects(userDetails.getSubjects())
              .changePassword(userDetails.isChangePassword())
              .financialBackground(userDetails.getFinancialBackground())
              .build();

      return ResponseEntity.ok()
          .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
          .body(response);

    } catch (Exception e) {
      logger.error("Authentication failed: ", e);
      throw new LearnBridgeException(e.getMessage(), e);
    }
  }

  @Override
  public ResponseEntity<Object> signup(SignupRequest request) throws LearnBridgeException {
    try {
      Optional<User> userByEmail = userRepository.findByEmail(request.getEmail());
      if (userByEmail.isPresent()) {
        return ResponseEntity.badRequest()
            .body(new SignupResponse(false, "Error: Email is already in use!", null));
      }

      Set<Role> roles = new HashSet<>();

      Role userRole =
          roleRepository
              .findByName(ERole.ROLE_USER)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

      roles.add(userRole);

      User user =
          User.builder()
              .name(request.getName())
              .surname(request.getSurname())
              .phoneNumber(request.getPhoneNumber())
              .email(request.getEmail())
              .password(encoder.encode(request.getPassword()))
              .learnerNumber(
                  LearnBridgeUtil.generateLearnerNumber(
                      request.getName(),
                      request.getSurname(),
                      request.getPhoneNumber(),
                      new Date()))
              .createdDate(new Date())
              .roles(roles)
              .build();

      User savedUser = userRepository.save(user);
      asyncService.registrationNotification(savedUser, request.getPassword());
      return ResponseEntity.ok(
          new SignupResponse(true, "Profile created successfully!", savedUser.getId()));

    } catch (Exception e) {
      logger.error("Unable to register user: ", e);
      throw new LearnBridgeException(e.getMessage(), e);
    }
  }

  @Override
  public ResponseEntity<Object> profileSetup(ProfileSetupRequest request)
      throws LearnBridgeException {
    try {

      Optional<User> userOptional = userRepository.findById(request.getUserId());

      if (userOptional.isPresent()) {
        User user = userOptional.get();
        user.setInterests(request.getInterests());
        user.setProvince(request.getProvince());
        user.setGrade(request.getGrade());
        user.setSubjects(request.getSubjects());
        user.setFinancialBackground(request.getFinancialBackground());
        userRepository.save(user);
      } else {
        throw new LearnBridgeException("User not found");
      }

      return ResponseEntity.ok(new MessageResponse(true, "Profile setup successfully!"));

    } catch (Exception e) {
      logger.error("Unable to setup user profile: ", e);
      throw new LearnBridgeException(e.getMessage(), e);
    }
  }

  @Override
  public ResponseEntity<Object> forgotPassword(ForgotPasswordRequest request) {
    try {
      String mssg =
          "Your password has been successfully reset. "
              + "An email has been sent to the registered email address "
              + "associated with your account containing the new login details. "
              + "Please check your email inbox.";

      Optional<User> userOptional =
          userRepository.findByEmailAndLearnerNumber(
              request.getEmail(), request.getLearnerNumber());
      if (userOptional.isEmpty()) {
        throw new NotFoundException(
            "User is not registered. Please contact us to register or check your credentials.");
      } else {
        asyncService.notifyUserNewPassword(userOptional.get());
      }

      return ResponseEntity.ok(ResponseEntity.ok(new MessageResponse(true, mssg)));
    } catch (NotFoundException e) {
      logger.error("Error: ", e);
      return ResponseEntity.badRequest().body(new MessageResponse(false, e.getMessage()));
    } catch (Exception e) {
      logger.error("Error: ", e);
      return ResponseEntity.ok(new MessageResponse(false, ConstantUtil.SERVICE_UNAVAILABLE_MSSG));
    }
  }

  public static List<String> getRoleFriendlyNames(List<String> roleNames) {
    return roleNames.stream().map(name -> ERole.valueOf(name).getValue()).toList();
  }
}
