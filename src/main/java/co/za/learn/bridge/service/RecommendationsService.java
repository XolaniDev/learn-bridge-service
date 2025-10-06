package co.za.learn.bridge.service;

import co.za.learn.bridge.excption.NotFoundException;
import co.za.learn.bridge.model.entity.Recommendations;
import co.za.learn.bridge.model.entity.User;
import co.za.learn.bridge.model.payload.response.CourseDto;
import co.za.learn.bridge.model.payload.response.FundingDto;
import co.za.learn.bridge.model.payload.response.JobTrendDto;
import co.za.learn.bridge.model.payload.response.RecommendationDto;
import co.za.learn.bridge.repository.AppConfigRepository;
import co.za.learn.bridge.repository.RecommendationsRepository;
import co.za.learn.bridge.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RecommendationsService {

  private final AppConfigRepository repository;
  private final UserRepository userRepository;
  private final RecommendationsRepository recommendationsRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public RecommendationDto getRecommendations(
      String grade, List<String> subjects, List<String> interests) {

    OpenAiService openAiService =
        new OpenAiService(
            repository.findByCode("OPENAI_API_KEY").getDescription(), Duration.ofSeconds(180));

    String prompt =
        String.format(
            """
       You are an AI assistant that helps South African high school learners explore study and career options.
       Based on the following student details, suggest 3–5 university or college courses in South Africa AND include 3–5 related job market trends.

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
          ],
          "jobTrends": [
            {
              "title": "Job Title",
              "demand": "Low | Medium | High | Very High",
              "salary": "e.g. R30,000 - R70,000"
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
      record AIResponse(List<CourseDto> recommendedCourses, List<JobTrendDto> jobTrends) {}

      AIResponse response = objectMapper.readValue(content, new TypeReference<AIResponse>() {});
      return RecommendationDto.builder()
          .recommendedCourses(response.recommendedCourses())
          .jobTrends(response.jobTrends())
          .build();

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

      List<FundingDto> fundings =
          List.of(
              new FundingDto("NSFAS Bursary", "Government", "Full Coverage"),
              new FundingDto("Sasol Bursary", "Corporate", "R80,000/year"),
              new FundingDto("Allan Gray Orbis Foundation", "Private", "Full Coverage"));

      List<FundingDto> fundingOpportunities = new ArrayList<>(); // TODO: Integrate funding service

      Recommendations recommendations =
          Recommendations.builder()
              .userId(userId)
              .recommendedCourses(recommendationDto.getRecommendedCourses())
              .jobTrends(recommendationDto.getJobTrends())
              .fundingOpportunities(fundingOpportunities)
              .createdDate(new Date())
              .build();

      Optional<Recommendations> rec = recommendationsRepository.findByUserId(userId);

      recommendationsRepository.save(recommendations);
      rec.ifPresent(recommendationsRepository::delete);

      return recommendations;
    } else {
      throw new NotFoundException("User not found with ID: " + userId);
    }
  }

  public Recommendations getUserRecommendations(String userId) {

    Optional<Recommendations> recommendation = recommendationsRepository.findByUserId(userId);

    return recommendation.orElseGet(() -> generateRecommendations(userId));
  }
}
