package co.za.learn.bridge.controller;

import co.za.learn.bridge.model.entity.FundingDetails;
import co.za.learn.bridge.repository.FundingRepository;
import co.za.learn.bridge.service.FundingCrawlerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funding-crawler")
public class FundingCrawlerController {

    private final FundingCrawlerService crawlerService;
    private final FundingRepository repository;

    public FundingCrawlerController(FundingCrawlerService crawlerService, FundingRepository repository) {
        this.crawlerService = crawlerService;
        this.repository = repository;
    }

    @PostMapping("/crawl")
    public ResponseEntity<List<FundingDetails>> startCrawl(@RequestBody(required = false) List<String> categories) {
        List<String> categoryUrls = categories != null && !categories.isEmpty() ? categories : List.of(
            "https://allbursaries.co.za/engineering",
            "https://allbursaries.co.za/medical",
            "https://allbursaries.co.za/accounting",
            "https://allbursaries.co.za/education-teaching",
            "https://allbursaries.co.za/agriculture",
            "https://allbursaries.co.za/general"
        );
        List<FundingDetails> result = crawlerService.crawlCategories(categoryUrls);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<FundingDetails>> listAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FundingDetails> getOne(@PathVariable String id) {
        return repository.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
