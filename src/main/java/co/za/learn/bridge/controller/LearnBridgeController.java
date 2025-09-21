package co.za.learn.bridge.controller;

import co.za.learn.bridge.model.payload.request.UpdateLoginDetailsRequest;
import co.za.learn.bridge.model.payload.request.UpdateUserRequest;
import co.za.learn.bridge.service.LearnBridgeService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/lb")
@CrossOrigin(origins = "*", maxAge = 3600)
public class LearnBridgeController {

  private static final Logger logger = LogManager.getLogger(LearnBridgeController.class);
  private LearnBridgeService learnBridgeService;

  @PostMapping("/update-user")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<Object> updateUser(@Valid @RequestBody UpdateUserRequest request) {
    logger.info("Update user, FarmID: {}, Name: {}", request.getFarmId(), request.getName());
    return learnBridgeService.updateUser(request);
  }

  @PostMapping("/update-login-details")
  @PreAuthorize("hasRole('USER') or hasRole('FARM_MANAGER') or hasRole('FARM_WORKER')")
  ResponseEntity<Object> updateLoginDetails(@Valid @RequestBody UpdateLoginDetailsRequest request) {
    logger.info("Update login details: User ID: {}", request.getUserId());
    return learnBridgeService.updateLoginDetails(request);
  }
}
