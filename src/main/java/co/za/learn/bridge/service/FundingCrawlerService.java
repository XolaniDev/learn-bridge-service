package co.za.learn.bridge.service;

import co.za.learn.bridge.model.entity.FundingDetails;
import co.za.learn.bridge.repository.FundingRepository;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

@Service
public class FundingCrawlerService {

    private final FundingRepository repository;
    private final String USER_AGENT = "LearnBridgeBot/1.0 (+https://yourdomain.example)"; // be honest
    private final int RATE_LIMIT_MS = 1200; // 1.2s between requests (tweak as needed)
    private final AtomicInteger idCounter = new AtomicInteger(1000);

    public FundingCrawlerService(FundingRepository repository) {
        this.repository = repository;
    }

    /**
     * Crawl a list of category urls and extract funding details.
     */
    public List<FundingDetails> crawlCategories(List<String> categoryUrls) {
        List<FundingDetails> results = new ArrayList<>();

        for (String categoryUrl : categoryUrls) {
          /*  if (!isAllowedByRobots(categoryUrl)) {
                // skip category if disallowed
                continue;
            }*/
            try {
                Document catDoc = fetchDocument(categoryUrl);
                // Heuristic: links to bursary detail pages often appear as <a> inside article or .listing
                Elements links = catDoc.select("a[href]");
                Set<String> detailUrls = new LinkedHashSet<>();
                for (Element a : links) {
                    String href = a.absUrl("href");
                    if (href == null || href.isEmpty()) continue;
                    // only include internal pages on allbursaries.co.za and filter probable detail pages
                    if (href.contains("allbursaries.co.za") && looksLikeBursaryPage(href, categoryUrl)) {
                        detailUrls.add(href.split("#")[0]); // strip anchors
                    }
                }

                for (String detailUrl : detailUrls) {
                    try {
                        Thread.sleep(RATE_LIMIT_MS);
                        FundingDetails dto = parseFundingDetail(detailUrl);
                        if (dto != null) {
                            // dedupe by name + website or url
                            String key = dto.getName() + "|" + (dto.getWebsite() == null ? detailUrl : dto.getWebsite());
                            // assign id if missing
                            if (dto.getId() == null) dto.setId(String.valueOf(idCounter.getAndIncrement()));
                            if (!repository.existsById(dto.getId())) {
                                repository.save(dto);
                                results.add(dto);
                            } else {
                                // optionally update existing record
                                repository.save(dto);
                            }
                        }
                    } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                    catch (Exception e) {
                        // log and continue with next listing
                        System.err.println("Failed to parse detail page: " + detailUrl + " -> " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to fetch category: " + categoryUrl + " -> " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Basic heuristic to detect bursary detail pages.
     * On allbursaries the category pages often link to detail pages with '/[slug]' or '/bursary-name' patterns.
     */
    private boolean looksLikeBursaryPage(String href, String categoryUrl) {
        // simple heuristics: include if same domain and not just the category itself
        if (!href.startsWith("https://allbursaries.co.za")) return false;
        if (href.equals(categoryUrl) || href.equals(categoryUrl + "/")) return false;
        // avoid pagination or assets
        if (href.contains("/page/") || href.contains("/tag/")) return false;
        // avoid root
        if (href.equals("https://allbursaries.co.za/")) return false;
        // typically detail pages are under root with a slug (one or two path segments)
        return true;
    }

    /** Fetch a page using Jsoup with a respectful user-agent and timeout. */
    private Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .referrer("https://google.com")
                .timeout(30_000)
                .get();
    }

    /** Parse a bursary detail page using a number of selectors and heuristics. */
    public FundingDetails parseFundingDetail(String url) throws IOException {
        Document doc = fetchDocument(url);

        FundingDetails dto = new FundingDetails();
        dto.setId(String.valueOf(idCounter.getAndIncrement()));
        dto.setWebsite(url); // default to the detail page

        // Title - try common selectors
        String title = firstNonEmpty(
                () -> doc.selectFirst("h1.entry-title") != null ? doc.selectFirst("h1.entry-title").text() : null,
                () -> doc.selectFirst("h1") != null ? doc.selectFirst("h1").text() : null,
                () -> doc.title()
        );
        dto.setName(title != null ? title.trim() : "Unknown");

        // Description - try common article element
        Element article = doc.selectFirst("article");
        String description = null;
        if (article != null) {
            // pick first couple of <p> elements as a description
            Elements ps = article.select("p");
            if (!ps.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(3, ps.size()); i++) {
                    sb.append(ps.get(i).text()).append("\n\n");
                }
                description = sb.toString().trim();
            }
        }
        if (description == null || description.isEmpty()) {
            description = firstNonEmpty(
                    () -> doc.selectFirst(".entry-content") != null ? doc.selectFirst(".entry-content").text() : null,
                    () -> doc.selectFirst(".post-content") != null ? doc.selectFirst(".post-content").text() : null
            );
        }
        dto.setDescription(trimToLength(description, 3000));

        // requirements - look for headings "Requirements" "Eligibility" then collect next <ul> or <p>
        List<String> requirements = extractListAfterHeading(doc, Arrays.asList("Requirements", "Eligibility", "Who can apply", "Eligibility Criteria"));
        if (requirements.isEmpty()) {
            // fallback: look for lists with keywords
            requirements = pickListContainingKeywords(doc, Arrays.asList("South African", "must", "require", "minimum", "age", "citizen"));
        }
        dto.setRequirements(requirements);

        // fields (fields of study) - look for "Fields", "Study", categories
        List<String> fields = extractListAfterHeading(doc, Arrays.asList("Fields of study", "Fields", "Study", "Target courses"));
        dto.setFields(fields.isEmpty() ? List.of() : fields);

        // coverage - what's covered
        List<String> coverage = extractListAfterHeading(doc, Arrays.asList("What it covers", "Coverage", "Includes", "What's covered"));
        dto.setCoverage(coverage);

        // deadline - regex search of page text
        String pageText = doc.text();
        String deadline = findDateLikeString(pageText);
        dto.setDeadline(deadline);

        // amount - search keywords
        String amount = findAmountString(pageText);
        dto.setAmount(amount);

        // criteria detection (very simple heuristics)
        String criteria = detectCriteria(pageText);
        dto.setCriteria(criteria);

        // try to find external official website link (prefer links pointing outside allbursaries)
        dto.setWebsite(findExternalLink(doc, "allbursaries.co.za"));

        // color - set default based on type or category (not critical)
        dto.setColor("bg-blue-500");

        // type - try to infer Government / Corporate / Private from content or title
        String type = inferTypeFromText(title + " " + pageText);
        dto.setType(type);

        return dto;
    }

    // ---------- Helper methods ----------

    private String inferTypeFromText(String text) {
        text = text.toLowerCase(Locale.ROOT);
        if (text.contains("nsfas") || text.contains("government") || text.contains("department")) return "Government";
        if (text.contains("bursary programme") || text.contains("corporate") || text.contains("bursary programme") || text.contains("sponsor")) return "Corporate";
        if (text.contains("foundation") || text.contains("foundation") || text.contains("private")) return "Private";
        return "Unknown";
    }

    private String findExternalLink(Document doc, String internalDomain) {
        Elements links = doc.select("a[href]");
        for (Element a : links) {
            String href = a.absUrl("href");
            if (href == null || href.isEmpty()) continue;
            if (!href.contains(internalDomain)) {
                // choose first non-internal link that looks like an "apply" or external official site
                String text = a.text().toLowerCase();
                if (text.contains("apply") || text.contains("official") || text.contains("website") || href.contains("http")) {
                    return href;
                }
            }
        }
        // fallback to the page url
        return doc.baseUri();
    }

    private String detectCriteria(String pageText) {
        String lower = pageText.toLowerCase(Locale.ROOT);
        if (lower.contains("merit") || lower.contains("70%") || lower.contains("academic")) return "Merit-based";
        if (lower.contains("income") || lower.contains("sassa") || lower.contains("household income") || lower.contains("need")) return "Need-based";
        if (lower.contains("must") && lower.contains("financial")) return "Mixed";
        return "Unknown";
    }

    private String findAmountString(String text) {
        // look for currency patterns like R80,000 or "full" coverage
        Pattern p = Pattern.compile("(R\\s?\\d[\\d,\\.]+(?:\\s?[-to]+\\s?R\\s?\\d[\\d,\\.]+)?)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);
        if (text.toLowerCase().contains("full coverage") || text.toLowerCase().contains("full bursary")) return "Full Coverage";
        return null;
    }

    private String findDateLikeString(String text) {
        // Simple date regex dd Month yyyy or dd Month yy or yyyy-mm-dd
        Pattern p = Pattern.compile("(\\d{1,2}\\s+(January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);
        // ISO style
        p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
        m = p.matcher(text);
        if (m.find()) return m.group(1);
        // fallback: find month + year mentions
        p = Pattern.compile("(January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{4}", Pattern.CASE_INSENSITIVE);
        m = p.matcher(text);
        if (m.find()) return m.group(0);
        return null;
    }

    private List<String> pickListContainingKeywords(Document doc, List<String> keywords) {
        Elements uls = doc.select("ul");
        for (Element ul : uls) {
            String text = ul.text().toLowerCase();
            for (String kw : keywords) {
                if (text.contains(kw.toLowerCase())) {
                    List<String> items = new ArrayList<>();
                    for (Element li : ul.select("li")) items.add(li.text());
                    if (!items.isEmpty()) return items;
                }
            }
        }
        return Collections.emptyList();
    }

    private List<String> extractListAfterHeading(Document doc, List<String> headings) {
        for (String h : headings) {
            // find heading elements with text equal to h (h2,h3, strong etc)
            Elements candidates = doc.select("h2, h3, h4, strong, b");
            for (Element c : candidates) {
                if (c.text().toLowerCase().contains(h.toLowerCase())) {
                    // look for next sibling ul or p
                    Element nextUl = nextSiblingWithTag(c, "ul");
                    if (nextUl != null) {
                        List<String> items = new ArrayList<>();
                        for (Element li : nextUl.select("li")) items.add(li.text());
                        if (!items.isEmpty()) return items;
                    }
                    // fallback to reading sibling paragraphs
                    Element nextP = nextSiblingWithTag(c, "p");
                    if (nextP != null) {
                        return Arrays.asList(nextP.text().split("\\.\\s+"));
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private Element nextSiblingWithTag(Element e, String tag) {
        Element sibling = e.nextElementSibling();
        while (sibling != null) {
            if (sibling.tagName().equalsIgnoreCase(tag)) return sibling;
            sibling = sibling.nextElementSibling();
        }
        return null;
    }

    private String trimToLength(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    @SafeVarargs
    private final <T> T firstNonEmpty(SupplierWithException<T>... suppliers) {
        for (SupplierWithException<T> s : suppliers) {
            try {
                T t = s.get();
                if (t != null && !(t instanceof String && ((String) t).isEmpty())) return t;
            } catch (Exception ignored) {}
        }
        return null;
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws Exception;
    }

    /**
     * Basic robots.txt allowance check (simple).
     * This is a lightweight check - for production you should use a robust robots parser.
     */
    private boolean isAllowedByRobots(String url) {
        try {
            URL u = new URL(url);
            String robotsUrl = u.getProtocol() + "://" + u.getHost() + "/robots.txt";
            Document robots = Jsoup.connect(robotsUrl).userAgent(USER_AGENT).timeout(5000).ignoreHttpErrors(true).get();
            String robotsText = robots.body().text().toLowerCase();
            // extremely simple check: if it contains "disallow: /" then block
            if (robotsText.contains("disallow: /")) {
                return false;
            }
        } catch (Exception ignored) { }
        return true;
    }
}
