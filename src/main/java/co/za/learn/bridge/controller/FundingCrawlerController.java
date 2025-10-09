package co.za.learn.bridge.controller;

import co.za.learn.bridge.model.entity.FundingDetails;
import co.za.learn.bridge.service.FundingCrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funding")
public class FundingCrawlerController {

    @Autowired
    private FundingCrawlerService crawlerService;

    @GetMapping("/crawl")
    public ResponseEntity<List<FundingDetails>> crawlFundingData() {
        List<FundingDetails> bursaries = crawlerService.crawlAndRefreshData();
        return ResponseEntity.ok(bursaries);
    }
}
