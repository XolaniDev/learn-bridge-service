package co.za.learn.bridge.service;

import co.za.learn.bridge.mail.Email;
import co.za.learn.bridge.mail.EmailSender;
import co.za.learn.bridge.model.entity.User;
import co.za.learn.bridge.repository.UserRepository;
import co.za.learn.bridge.utils.ConstantUtil;
import co.za.learn.bridge.utils.LearnBridgeUtil;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class AsyncService {
  private static final Logger LOGGER = LoggerFactory.getLogger(AsyncService.class);

  private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
  private final EmailSender emailSender;
  private UserRepository userRepository;

  @Async
  public void notifyUserNewPassword(User u) {
    // Generate password
    String pwd = LearnBridgeUtil.generatePassword();
    u.setPassword(bCryptPasswordEncoder.encode(pwd));
    u.setChangePassword(true);
    userRepository.save(u);

    String mssg =
        "<p>Dear #NAME#,</p>"
            + "<p>We received your request to reset your password. "
            + "As per your request, we have generated a new password for your account. "
            + "Your new login details are as follows:</p>"
            + "<ul>"
            + "<li><strong>Username:</strong> "
            + u.getEmail()
            + "</li>"
            + "<li><strong>New Password:</strong> "
            + pwd
            + "</li>"
            + "</ul>"
            + "<p>Please use this password to log in to your account. Once you have logged in, "
            + "you will be prompted to change your password for security reasons. "
            + "We recommend choosing a password that is both secure and easy for you to remember.</p>"
            + "<p>If you did not request a password reset, or if you experience any issues "
            + "logging in or changing your password, please contact us immediately for assistance. "
            + "We take the security of our user accounts very seriously and will work with you "
            + "to ensure that your account remains secure.</p>"
            + "<p>Thank you for being a valued member. We look forward to your continued participation "
            + "and engagement.</p>";

    mssg = mssg.replace("#NAME#", u.getName() + " " + u.getSurname());

    Email mail = new Email();

    mail.setContent(mssg);
    mail.setFrom(ConstantUtil.NO_REPLY_EMAIL);
    String[] to = {u.getEmail()};
    mail.setTo(to);
    mail.setSubject("New Password for Your Account");
    emailSender.saveEmail(mail);
  }

  @Async
  public void notifyUserPasswordChange(User u, String password) {
    String welcome =
        "<p>Dear #NAME#,</p>"
            + "<p>We hope this email finds you well. "
            + "We want to inform you that your password has been successfully updated for your Learner Bridge account. "
            + "Your new login details are as follows:</p>"
            + "<ul>"
            + "<li><strong>Username:</strong> "
            + u.getEmail()
            + "</li>"
            + "<li><strong>New Password:</strong> "
            + password
            + "</li>"
            + "</ul>"
            + "<p>If you did not initiate this password change, or if you suspect any unauthorized access to your account, "
            + "please contact our support team immediately for assistance.</p>"
            + "<p>As always, please remember to keep your login credentials secure and avoid sharing them with anyone. "
            + "Our team will never ask you to disclose your password or personal information via email or any other means.</p>"
            + "<p>Thank you for being a valued member. We look forward to your continued participation and engagement.</p>";

    welcome = welcome.replace("#NAME#", u.getName() + " " + u.getSurname());

    Email mail = new Email();

    mail.setContent(welcome);
    mail.setFrom(ConstantUtil.NO_REPLY_EMAIL);
    String[] to = {u.getEmail()};
    mail.setTo(to);
    mail.setSubject("Password Updated Successfully");
    emailSender.saveEmail(mail);
  }

  @Async
  public void registrationNotification(User u, String plainPass) {

    String welcome =
        "<p>Dear #NAME#,</p>"
            + "<p>We are delighted to inform you that your Learner Bridge "
            + "account has been successfully created. Your account details are as follows:</p>"
            + "<ul>"
                + "<li><strong>Learner Number: </strong> "
                + u.getLearnerNumber()
            + "<li><strong>Username: </strong> "
            + u.getEmail()
            + "</li>"
            + "<li><strong>Password:</strong> "
            + plainPass
            + "</li>"
            + "</ul>"
            + "<p>Please use these details to log in to your account. "
            + "<p>If you have any questions or need assistance with "
            + "your account, please do not hesitate to contact us. "
            + "We are always happy to help you.</p>"
            + "<p>Thank you for joining Learner Bridge. "
            + "We look forward to work with  you.</p>";

    welcome = welcome.replace("#NAME#", u.getName() + " " + u.getSurname());

    Email mail = new Email();


    mail.setContent(welcome);
    mail.setFrom(ConstantUtil.NO_REPLY_EMAIL);
    String[] to = {u.getEmail()};
    mail.setTo(to);
    mail.setSubject("Learner Bridge Account Created");
    emailSender.saveEmail(mail);
  }
}
