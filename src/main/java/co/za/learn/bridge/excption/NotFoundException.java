package co.za.learn.bridge.excption;

public class NotFoundException extends RuntimeException {
  public NotFoundException(String errorMessage) {
    super(errorMessage);
  }
}
