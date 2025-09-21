package co.za.learn.bridge.service;

import co.za.learn.bridge.model.payload.request.LoginRequest;
import co.za.learn.bridge.model.payload.request.SignupRequest;
import co.za.learn.bridge.utils.exception.LearnBridgeException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface AuthControllerService {
     ResponseEntity<Object> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) throws LearnBridgeException;
     ResponseEntity<Object> signup(@Valid @RequestBody SignupRequest request) throws LearnBridgeException;
}
