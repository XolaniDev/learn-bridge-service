package co.za.learn.bridge.service;

import co.za.learn.bridge.excption.NotFoundException;
import co.za.learn.bridge.model.entity.FundingDetails;
import co.za.learn.bridge.model.entity.Recommendations;
import co.za.learn.bridge.model.entity.User;
import co.za.learn.bridge.model.entity.UserFundings;
import co.za.learn.bridge.model.payload.response.*;
import co.za.learn.bridge.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class OpenAiService {

  private final AppConfigRepository repository;
  private final UserRepository userRepository;
  private final FundingRepository fundingRepository;
  private final UserFundingsRepository userFundingsRepository;
  private final RecommendationsRepository recommendationsRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public RecommendationDto getRecommendations(
      String grade, List<String> subjects, List<String> interests) {

    com.theokanning.openai.service.OpenAiService openAiService =
        new com.theokanning.openai.service.OpenAiService(
            repository.findByCode("OPENAI_API_KEY").getDescription(), Duration.ofSeconds(180));

    String prompt =
        String.format(
            """
       You are an AI assistant that helps South African high school learners explore study and career options.
       Based on the following student details, suggest 3–5 university or college courses in South Africa.

        Student Details:
        - Grade: %s
        - Subjects: %s
        - Interests: %s

        Return ONLY valid JSON (no markdown, no explanations) in the format:
        {
          "recommendedCourses": [
            {
              "id": "1",
              "name": "Course Name",
              "description": "Short course description",
              "requiredSubjects": ["Subject1", "Subject2"],
              "university": "University Name",
              "duration": "e.g. 3 years",
              "qualification": "Qualification Type"
            }
          ]
        }
        """,
            grade, String.join(", ", subjects), String.join(", ", interests));

    ChatCompletionRequest request =
        ChatCompletionRequest.builder()
            .model("gpt-4o-mini")
            .messages(List.of(new ChatMessage("user", prompt)))
            .maxTokens(900)
            .temperature(0.7)
            .build();

    // Execute request
    ChatCompletionResult result = openAiService.createChatCompletion(request);
    String content = result.getChoices().getFirst().getMessage().getContent();

    try {
      // Define a nested record for deserialization
      record AIResponse(List<CourseDto> recommendedCourses) {}

      AIResponse response = objectMapper.readValue(content, new TypeReference<AIResponse>() {});
      return RecommendationDto.builder().recommendedCourses(response.recommendedCourses()).build();

    } catch (Exception e) {
      throw new RuntimeException("Failed to parse AI recommendations: " + content, e);
    }
  }

  public Recommendations generateRecommendations(String userId) {

    Optional<User> optionalUser = userRepository.findById(userId);

    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      String grade = user.getGrade();
      List<String> subjects = user.getSubjects();
      List<String> interests = user.getInterests();

      RecommendationDto recommendationDto = getRecommendations(grade, subjects, interests);

      JobMarketResponse jobMarketData = getJobMarketData(recommendationDto.getRecommendedCourses());

        if (jobMarketData != null && jobMarketData.getJobsByCategory() != null) {
            jobMarketData.getJobsByCategory().forEach((category, jobList) -> {
                if (jobList != null) {
                    jobList.forEach(job -> {
                        job.setId(UUID.randomUUID().toString());
                        job.setLiked(false);
                    });
                }
            });
        }

      Recommendations recommendations =
          Recommendations.builder()
              .userId(userId)
              .recommendedCourses(recommendationDto.getRecommendedCourses())
              .jobTrends(getAllJobs(jobMarketData))
              .jobMarket(jobMarketData)
              .careerGrowthTips(generateCareerGrowthTips(recommendationDto.getRecommendedCourses()))
              .createdDate(new Date())
              .build();

      Optional<Recommendations> rec = recommendationsRepository.findByUserId(userId);

      recommendationsRepository.save(recommendations);
      rec.ifPresent(recommendationsRepository::delete);

      List<UserFundings> userFundingsList = userFundingsRepository.findByUserId(userId);
      if (userFundingsList != null && !userFundingsList.isEmpty()) {
        userFundingsRepository.deleteAll(userFundingsList);
      }

      return recommendations;
    } else {
      throw new NotFoundException("User not found with ID: " + userId);
    }
  }

  public List<JobDto> getAllJobs(JobMarketResponse response) {
    if (response == null || response.getJobsByCategory() == null) {
      return List.of();
    }

    return response
        .getJobsByCategory()
        .values() // Get all List<JobDto> values
        .stream()
        .flatMap(List::stream) // Flatten all lists into a single stream
        .collect(Collectors.toList()); // Collect into one List<JobDto>
  }

  public Recommendations getUserRecommendations(String userId) {

    Optional<Recommendations> recommendation = recommendationsRepository.findByUserId(userId);
    Recommendations recommendations =
        recommendation.orElseGet(() -> generateRecommendations(userId));

    List<FundingDto> fundings =
        findFundingDetails(recommendations.getRecommendedCourses(), userId).stream()
            .map(
                f ->
                    FundingDto.builder()
                        .name(f.getName())
                        .type(f.getType())
                        .amount(f.getAmount())
                        .build())
            .limit(3)
            .toList();

    recommendations.setFundingOpportunities(fundings);

    return recommendations;
  }

  public List<String> getFieldOfStudies(String url) {
    // Build the prompt
    String prompt =
        """
      You are an expert in bursaries and educational funding.\s

      Given the following bursary website URL, determine the most relevant **fields of study** for which this bursary is applicable. Only use the information available on the website to decide. Return the result as a JSON array of strings.

      Website URL: %s

      Instructions:
      - Identify the most relevant fields of study (e.g., Accounting, Finance, Auditing, Commerce).
      - Return **only** a JSON array of strings, without any extra text.
      - Example output: ["Accounting", "Finance", "Auditing"]
      """
            .formatted(url);

    com.theokanning.openai.service.OpenAiService openAiService =
        new com.theokanning.openai.service.OpenAiService(
            repository.findByCode("OPENAI_API_KEY").getDescription(), Duration.ofSeconds(180));

    // Create chat request
    ChatCompletionRequest request =
        ChatCompletionRequest.builder()
            .model("gpt-4o-mini")
            .messages(List.of(new ChatMessage("user", prompt)))
            .maxTokens(900)
            .temperature(0.7)
            .build();

    // Execute request
    ChatCompletionResult result = openAiService.createChatCompletion(request);
    String content = result.getChoices().get(0).getMessage().getContent().trim();

    // Parse JSON array returned by the model into a Java List
    try {
      // Remove potential extra characters like quotes, whitespace
      if (!content.startsWith("[")) {
        int start = content.indexOf("[");
        int end = content.lastIndexOf("]");
        if (start != -1 && end != -1) {
          content = content.substring(start, end + 1);
        }
      }

      // Convert JSON array string to List<String>
      return Arrays.stream(
              content
                  .replaceAll("\\[|\\]|\"", "") // remove brackets and quotes
                  .split("\\s*,\\s*")) // split by comma
          .filter(s -> !s.isEmpty())
          .collect(Collectors.toList());
    } catch (Exception e) {
      e.printStackTrace();
      return List.of(); // fallback empty list
    }
  }

  public JobMarketResponse getJobMarketData(List<CourseDto> courses) {
    try {
      // Initialize OpenAI service
      com.theokanning.openai.service.OpenAiService openAiService =
          new com.theokanning.openai.service.OpenAiService(
              repository.findByCode("OPENAI_API_KEY").getDescription(), Duration.ofSeconds(180));

      // Convert courses list to JSON
      ObjectMapper mapper = new ObjectMapper();
      String coursesJson = mapper.writeValueAsString(courses);

      // AI Prompt
      String prompt =
          "You are an expert in education and job market analysis.\n\n"
              + "Given the following list of recommended courses in JSON format:\n"
              + coursesJson
              + "\n\nAnalyze them and generate job market insights relevant to South Africa.\n"
              + "Return ONLY valid JSON with this structure:\n\n"
              + "{\n"
              + "  \"jobsByCategory\": {\n"
              + "     \"categoryName\": [\n"
              + "       {\n"
              + "         \"title\": \"Job Title\",\n"
              + "         \"industry\": \"Industry\",\n"
              + "         \"demand\": \"High/Medium/Low\",\n"
              + "         \"salary\": \"Salary Range (e.g. R30,000 - R90,000)\",\n"
              + "         \"growth\": \"+X% annually\",\n"
              + "         \"description\": \"Job summary\",\n"
              + "         \"requirements\": [\"Skill1\", \"Skill2\"],\n"
              + "         \"companies\": [\"Company1\", \"Company2\"],\n"
              + "         \"education\": \"Typical qualification required\",\n"
              + "         \"hotspots\": [\"Province1\", \"Province2\"]\n"
              + "       }\n"
              + "     ]\n"
              + "  },\n"
              + "  \"marketInsights\": {\n"
              + "     \"fastestGrowingSectors\": [\n"
              + "       {\"name\": \"Sector Name\", \"growth\": \"+X%\", \"jobs\": \"Y new positions\"}\n"
              + "     ],\n"
              + "     \"inDemandSkills\": [\"Skill1\", \"Skill2\", \"Skill3\"]\n"
              + "  }\n"
              + "}\n\n"
              + "Return ONLY raw JSON — no explanations or markdown formatting.";

      // Create request
      ChatCompletionRequest request =
          ChatCompletionRequest.builder()
              .model("gpt-4o-mini")
              .messages(List.of(new ChatMessage("user", prompt)))
              .maxTokens(1500)
              .temperature(0.7)
              .build();

      // Execute request
      ChatCompletionResult result = openAiService.createChatCompletion(request);
      String content = result.getChoices().getFirst().getMessage().getContent();

      // --- 🧹 Clean up response before parsing ---
      if (content != null) {
        content = content.trim();

        // Remove Markdown-style ```json ... ``` wrappers if present
        if (content.startsWith("```")) {
          content = content.replaceAll("(?s)```(json)?", "").trim();
        }

        // Remove any leading text before JSON begins
        int startIndex = content.indexOf("{");
        if (startIndex > 0) {
          content = content.substring(startIndex);
        }

        // Remove trailing non-JSON text
        int endIndex = content.lastIndexOf("}");
        if (endIndex > 0 && endIndex < content.length() - 1) {
          content = content.substring(0, endIndex + 1);
        }
      }

      // Parse JSON into JobMarketResponse
      return mapper.readValue(content, JobMarketResponse.class);

    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Failed to fetch job market data from AI: " + e.getMessage());
      return new JobMarketResponse(); // fallback empty
    }
  }

  public List<String> generateCareerGrowthTips(List<CourseDto> courses) {
    try {
      // Initialize OpenAI service
      com.theokanning.openai.service.OpenAiService openAiService =
          new com.theokanning.openai.service.OpenAiService(
              repository.findByCode("OPENAI_API_KEY").getDescription(), Duration.ofSeconds(120));

      // Convert courses list to JSON
      ObjectMapper mapper = new ObjectMapper();
      String coursesJson = mapper.writeValueAsString(courses);

      // Build AI prompt
      String prompt =
          "You are a career advisor helping students choose better career paths.\n\n"
              + "Given the following list of courses in JSON format:\n"
              + coursesJson
              + "\n\nBased on these courses, generate a short list of career growth tips for students in South Africa.\n"
              + "Each tip should be concise and motivational, like:\n"
              + "• Keep updating your technical skills.\n"
              + "• Gain practical experience through internships.\n"
              + "• Network with professionals in your field.\n\n"
              + "Return ONLY a valid JSON array of strings, without code blocks or extra text.\n"
              + "Example: [\"Tip 1\", \"Tip 2\", \"Tip 3\", \"Tip 4\"]";

      // Create chat completion request
      ChatCompletionRequest request =
          ChatCompletionRequest.builder()
              .model("gpt-4o-mini")
              .messages(List.of(new ChatMessage("user", prompt)))
              .maxTokens(400)
              .temperature(0.7)
              .build();

      // Send to AI
      ChatCompletionResult result = openAiService.createChatCompletion(request);
      String content = result.getChoices().getFirst().getMessage().getContent();

      // 🧹 Clean unwanted markdown characters
      content =
          content
              .replaceAll("(?s)^```(?:json)?", "") // remove starting code fence
              .replaceAll("```$", "") // remove ending code fence
              .replaceAll("`", "") // remove stray backticks
              .trim();

      // Parse the JSON array returned by AI into a list of strings
      return mapper.readValue(content, new TypeReference<List<String>>() {});

    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Failed to generate career growth tips: " + e.getMessage());
      return List.of(
          "Stay adaptable — the job market is always evolving.",
          "Keep learning new technologies and soft skills.",
          "Look for internship or volunteer opportunities to gain experience.");
    }
  }

  public List<FundingDetails> findFundingDetails(List<CourseDto> courses, String userId) {
    Set<FundingDetails> relevantBursaries = new HashSet<>();
    List<FundingDetails> bursaries = fundingRepository.findAll();
    try {

      List<UserFundings> userFundings = userFundingsRepository.findByUserId(userId);

      if (userFundings != null && !userFundings.isEmpty()) {
        List<FundingDetails> fundingDetailsList = new ArrayList<>();
        for (UserFundings uf : userFundings) {
          FundingDetails fd = new FundingDetails();
          try {
            BeanUtils.copyProperties(uf, fd);
          } catch (Exception e) {
            e.printStackTrace();
          }
          fundingDetailsList.add(fd);
        }
        return fundingDetailsList;
      }

      // Step 0: Add universal bursaries (like NSFAS)
      bursaries.stream()
          .filter(b -> b.getName().toLowerCase().contains("nsfas"))
          .forEach(relevantBursaries::add);

      // Step 1: Local matching based on fields and keywords
      for (CourseDto course : courses) {
        List<FundingDetails> matched =
            bursaries.stream()
                .filter(b -> !b.getName().toLowerCase().contains("nsfas")) // skip NSFAS
                .filter(
                    b ->
                        b.getFields().stream()
                            .anyMatch(
                                field ->
                                    course.getName().toLowerCase().contains(field.toLowerCase())
                                        || course
                                            .getDescription()
                                            .toLowerCase()
                                            .contains(field.toLowerCase())))
                .collect(Collectors.toList());

        if (matched.isEmpty()) {
          matched =
              bursaries.stream()
                  .filter(b -> !b.getName().toLowerCase().contains("nsfas"))
                  .filter(b -> b.getKeywords() != null && !b.getKeywords().isEmpty())
                  .filter(
                      b ->
                          b.getKeywords().stream()
                              .anyMatch(
                                  k -> course.getName().toLowerCase().contains(k.toLowerCase())))
                  .toList();
        }

        relevantBursaries.addAll(matched);
      }

      // Step 2: AI-enhanced mapping for courses with no matches
      List<CourseDto> unmappedCourses =
          courses.stream()
              .filter(
                  c ->
                      bursaries.stream()
                          .filter(b -> !b.getName().toLowerCase().contains("nsfas"))
                          .noneMatch(relevantBursaries::contains))
              .collect(Collectors.toList());

      if (!unmappedCourses.isEmpty()) {
        com.theokanning.openai.service.OpenAiService openAiService =
            new com.theokanning.openai.service.OpenAiService(
                repository.findByCode("OPENAI_API_KEY").getDescription(), Duration.ofSeconds(180));

        ObjectMapper mapper = new ObjectMapper();
        String bursariesJson =
            mapper.writeValueAsString(
                bursaries.stream()
                    .filter(b -> !b.getName().toLowerCase().contains("nsfas"))
                    .collect(Collectors.toList()));
        String coursesJson = mapper.writeValueAsString(unmappedCourses);

        String prompt =
            "You are an expert education analyst. Given the following lists:\n\n"
                + "Bursaries:\n"
                + bursariesJson
                + "\n\n"
                + "Courses:\n"
                + coursesJson
                + "\n\n"
                + "Match each course to the most relevant bursaries based on fields, keywords, and descriptions.\n"
                + "Return ONLY a JSON array of BursaryDto objects without any markdown or explanation.";

        ChatCompletionRequest request =
            ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(new ChatMessage("user", prompt)))
                .maxTokens(1500)
                .temperature(0.7)
                .build();

        ChatCompletionResult result = openAiService.createChatCompletion(request);
        String content =
            result
                .getChoices()
                .getFirst()
                .getMessage()
                .getContent()
                .replaceAll("(?s)^```(?:json)?", "")
                .replaceAll("```$", "")
                .replaceAll("`", "")
                .trim();

        List<FundingDetails> aiBursaries =
            mapper.readValue(content, new TypeReference<List<FundingDetails>>() {});
        relevantBursaries.addAll(aiBursaries);
      }

    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Failed to map bursaries to courses: " + e.getMessage());
    }

    List<UserFundings> userFundingsList =
        relevantBursaries.stream()
            .map(
                fd -> {
                  UserFundings uf = new UserFundings();
                  try {
                    BeanUtils.copyProperties(
                            fd,uf ); // copy fields from FundingDetails -> UserFundings
                  } catch (Exception e) {
                    e.printStackTrace();
                  }
                  return uf;
                })
            .collect(Collectors.toList());

    userFundingsRepository.saveAll(userFundingsList);

    // Return as a List, no duplicates
    return new ArrayList<>(relevantBursaries);
  }
}
