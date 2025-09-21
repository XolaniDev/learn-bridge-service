package co.za.learn.bridge.schedule;

import co.za.learn.bridge.mail.EmailSender;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class ScheduledTasks {

  private final EmailSender mailSender;


  /**
   * @Scheduled(cron = "[Seconds] [Minutes] [Hours] [Day of month] [Month] [Day of week] [Year]")
   * -------------------------------------------------------------------------------------------- @Scheduled(cron
   * = "0 * * * * ?")=Task will be executed every minute @Scheduled(cron = "0 0 12 * * ?")=Fires at
   * 12 PM every day @Scheduled(cron = "0 15 10 * * ? 2005")=Fires at 10:15 AM every day in the year
   * 2005 @Scheduled(cron = "0/20 * * * * ?")=Fires every 20 seconds
   * --------------------------------------------------------------------------------------------
   */
  @Scheduled(fixedDelay = 2000)
  public void scheduleTaskWithFixedDelay() {
    mailSender.sendEmailContent();
  }
  
}
