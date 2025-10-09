package co.za.learn.bridge.service;

import co.za.learn.bridge.model.entity.FundingDetails;
import co.za.learn.bridge.repository.FundingRepository;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.za.learn.bridge.repository.UserFundingsRepository;
import lombok.AllArgsConstructor;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FundingCrawlerService {

  private OpenAiService openAiService;

  static String baseUrl = "https://allbursaries.co.za";
  private static final List<String> CATEGORY_URLS =
      List.of(
          baseUrl,
          baseUrl + "/engineering",
          baseUrl + "/medical",
          baseUrl + "/accounting",
          baseUrl + "/education-teaching",
          baseUrl + "/agriculture",
          baseUrl + "/postgraduate",
          baseUrl + "/computer-science",
          baseUrl + "/general");

  private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1000);

  @Autowired private FundingRepository repository;
  @Autowired private UserFundingsRepository userFundingsRepository;

  public List<FundingDetails> crawlAndRefreshData() {
    List<FundingDetails> bursaries = new ArrayList<>();

    for (String categoryUrl : CATEGORY_URLS) {
      try {
        Document categoryDoc = Jsoup.connect(categoryUrl).timeout(20000).get();

        // Select all bursary article links for that category
        Elements links = categoryDoc.select("a[href*=-bursary]");

        List<String> bursaryLinks =
            links.stream()
                .map(link -> link.attr("href"))
                .filter(
                    href ->
                        href.contains(
                            categoryUrl
                                .replace("https://allbursaries.co.za/", "")
                                .replace("/", "")))
                .distinct()
                .toList();

        if (bursaryLinks.isEmpty()) {
          links = categoryDoc.select("a:matchesOwn(^Learn More$)");

          if (!links.isEmpty()) {
            bursaryLinks = links.stream().map(link -> link.attr("href")).distinct().toList();
            System.out.println("Href: " + bursaryLinks);
          }
        }

        System.out.println("Found " + bursaryLinks.size() + " bursary links in " + categoryUrl);
        bursaryLinks.forEach(System.out::println);

        for (String link : bursaryLinks) {
          String url = baseUrl.concat(link);
          FundingDetails dto = extractBursaryDetails(url, getType(categoryUrl));
          if (dto != null) {
            bursaries.add(dto);
          }
        }
      } catch (Exception e) {
        System.err.println("Failed to crawl category: " + categoryUrl + " -> " + e.getMessage());
      }
    }

    if (!bursaries.isEmpty()) {
      repository.deleteAll();
      userFundingsRepository.deleteAll();
      repository.saveAll(bursaries);
      System.out.println("Refreshed " + bursaries.size() + " bursary records.");
    } else {
      System.err.println("No bursary data found — skipping database update.");
    }

    return bursaries;
  }

  private String getType(String url) {
    if (url.equals(baseUrl)) return "Others";

    String path = url.replace(baseUrl + "/", "").replace("-", " ");
    path = path.replaceFirst("^/", ""); // remove leading slash if any

    // Capitalize first letter of each word
    String[] words = path.split("\\s+");
    StringBuilder description = new StringBuilder();
    for (String word : words) {
      if (!word.isEmpty()) {
        description
            .append(Character.toUpperCase(word.charAt(0)))
            .append(word.substring(1))
            .append(" ");
      }
    }
    return description.toString().trim();
  }

  private FundingDetails extractBursaryDetails(String url, String type) {
    try {
      Document doc = Jsoup.connect(url).timeout(20000).get();
      FundingDetails dto = new FundingDetails();
      dto.setId(String.valueOf(ID_GENERATOR.getAndIncrement()));
      dto.setType(type);
      dto.setWebsite(url);
      dto.setColor("bg-blue-500");
      dto.setCriteria("Merit-based");

      // --- Extract title ---
      String title = doc.title().split("-")[0].trim();
      dto.setName(title);

      // --- Extract description ---
      Element metaDesc = doc.selectFirst("meta[name=description]");
      dto.setDescription(metaDesc != null ? metaDesc.attr("content") : "");

      // --- Extract keywords ---
      Element keywordsMeta = doc.selectFirst("meta[name=keywords]");
      List<String> keywordsList =
          keywordsMeta != null
              ? Arrays.asList(keywordsMeta.attr("content").split("\\s*,\\s*"))
              : List.of();

      dto.setKeywords(keywordsList);

      dto.setFields(openAiService.getFieldOfStudies(dto.getWebsite()));

      // --- Extract amount and deadline from JSON-LD if present ---
      Element jsonScript = doc.selectFirst("script[type=application/ld+json]");
      if (jsonScript != null) {
        try {
          JSONObject jsonObject = new JSONObject(jsonScript.html());
          if (jsonObject.has("financialAid")) {
            JSONObject financialAid = jsonObject.getJSONObject("financialAid");
            dto.setAmount(financialAid.optString("amount", null));
            dto.setDeadline(financialAid.optString("applicationDeadline", null));
          }
        } catch (Exception ignored) {
        }
      }

      // --- Fallback: extract amount and deadline using regex ---
      String pageText = doc.text();
      if (dto.getDeadline() == null || dto.getDeadline().isEmpty()) {
        dto.setDeadline(
            extractWithRegex(
                pageText,
                "(?i)(closing date|deadline)[:\\s]+([0-9]{1,2}\\s+[A-Za-z]+\\s+[0-9]{4})",
                2,
                "N/A"));
      }
      if (dto.getAmount() == null || dto.getAmount().isEmpty()) {
        dto.setAmount(
            extractWithRegex(
                pageText, "(?i)(amount|value)[:\\s]+(R\\s?\\d+[\\d,]*)", 2, "Full tuition fees"));
      }

      // --- Extract requirements and coverage ---
      dto.setRequirements(
          extractListItemsByHeaders(
              doc, List.of("required", "requirement", "eligibility", "who can apply")));
      dto.setCoverage(
          extractListItemsByHeaders(doc, List.of("bursary benefits", "benefits", "covers")));

      // Fallback: take all <ul><li> items if requirements not found
      if (dto.getRequirements().isEmpty()) {
        dto.setRequirements(
            doc.select("div.entry-content ul li").eachText().stream()
                .filter(text -> text.length() > 5)
                .toList());
      }

      return dto;

    } catch (IOException e) {
      System.err.println("Failed to parse bursary page: " + url + " -> " + e.getMessage());
      return null;
    }
  }

  // --- Helper method to extract regex matches with default value ---
  private String extractWithRegex(String text, String regex, int group, String defaultValue) {
    Matcher matcher = Pattern.compile(regex).matcher(text);
    return matcher.find() ? matcher.group(group) : defaultValue;
  }

  // --- Helper method to extract <li> items following headers containing any keywords ---
  private List<String> extractListItemsByHeaders(Document doc, List<String> keywords) {
    List<String> list = new ArrayList<>();
    Elements headers = doc.select("h2, h3, h4");
    for (Element header : headers) {
      String headerText = header.text().toLowerCase();
      if (keywords.stream().anyMatch(headerText::contains)) {
        Element next = header.nextElementSibling();
        if (next != null && next.tagName().equals("ul")) {
          list.addAll(next.select("li").eachText());
        }
      }
    }
    return list;
  }
}
