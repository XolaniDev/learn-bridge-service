package co.za.learn.bridge.schedule;

import co.za.learn.bridge.mail.EmailSender;
import co.za.learn.bridge.service.FundingCrawlerService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class ScheduledTasks {

  private final EmailSender mailSender;

    @Autowired
    private FundingCrawlerService crawlerService;


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


    // Runs every day at 00:00 (midnight)
    @Scheduled(cron = "0 0 0 * * *")
    public void crawlAndRefreshData() {
        System.out.println("Daily task running at midnight -- Crawl And Refresh Data");
        crawlerService.crawlAndRefreshData();
    }
  
}
