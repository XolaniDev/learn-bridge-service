package co.za.learn.bridge.service;

import co.za.learn.bridge.model.payload.request.UpdateLoginDetailsRequest;
import co.za.learn.bridge.model.payload.request.UpdateUserRequest;
import org.springframework.http.ResponseEntity;

public interface LearnBridgeService {
  ResponseEntity<Object> updateUser(UpdateUserRequest request);
  ResponseEntity<Object> updateLoginDetails(UpdateLoginDetailsRequest request);
}
