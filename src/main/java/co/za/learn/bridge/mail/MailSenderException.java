package co.za.learn.bridge.mail;

public class MailSenderException extends Exception {
  public MailSenderException(String errorMessage) {
    super(errorMessage);
  }
}
