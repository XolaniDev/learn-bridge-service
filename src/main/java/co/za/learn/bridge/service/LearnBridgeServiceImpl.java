package co.za.learn.bridge.service;

import co.za.learn.bridge.model.entity.User;
import co.za.learn.bridge.model.payload.request.LoginRequest;
import co.za.learn.bridge.model.payload.request.UpdateLoginDetailsRequest;
import co.za.learn.bridge.model.payload.request.UpdateProfileSetupRequest;
import co.za.learn.bridge.model.payload.request.UpdateUserRequest;
import co.za.learn.bridge.model.payload.response.*;
import co.za.learn.bridge.repository.UserRepository;
import co.za.learn.bridge.utils.LearnBridgeUtil;
import co.za.learn.bridge.utils.ValidationUtil;
import co.za.learn.bridge.utils.exception.LearnBridgeException;
import java.util.*;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LearnBridgeServiceImpl implements LearnBridgeService {
  private static final Logger logger = LogManager.getLogger(LearnBridgeServiceImpl.class);
  private final UserRepository userRepository;
  private final PasswordEncoder encoder;
  private final AuthControllerService authControllerService;
  private final AsyncService asyncService;

  @Override
  public ResponseEntity<Object> updateUser(UpdateUserRequest request) {

    User user;
    try {

      Optional<User> userByEmail = userRepository.findByEmail(request.getEmail());

      if (userByEmail.isPresent()
          && !Objects.equals(userByEmail.get().getId(), request.getUserId())) {
        return ResponseEntity.badRequest()
            .body(new SignupResponse(false, "Error: Email is already in use!", null));
      }
      user = userRepository.save(getUser(request));
      logger.info("User update successful");
      return ResponseEntity.ok(LearnBridgeUtil.getUserInfoResponse(user));
    } catch (LearnBridgeException e) {
      logger.error("Update user Error: ", e);
      return ResponseEntity.badRequest().body(new MessageResponse(false, e.getMessage()));
    }
  }

  @Override
  public ResponseEntity<Object> updateProfileSetup(UpdateProfileSetupRequest request) {

    Optional<User> optionalUser = getUserOptional(request.getUserId());
    if (optionalUser.isPresent()) {
      User user = optionalUser.get();
      user.setInterests(request.getInterests());
      user.setProvince(request.getProvince());
      user.setGrade(request.getGrade());
      user.setSubjects(request.getSubjects());
      user.setFinancialBackground(request.getFinancialBackground());
      userRepository.save(user);
      logger.info("User update successful");
      return ResponseEntity.ok(LearnBridgeUtil.getUserInfoResponse(user));
    } else {
      return ResponseEntity.badRequest().body(new MessageResponse(false, "User not found"));
    }
  }

  @Override
  public ResponseEntity<Object> updateLoginDetails(UpdateLoginDetailsRequest request) {
    try {

      Optional<User> optionalUser = getUserOptional(request.getUserId());
      User user;
      if (optionalUser.isPresent()) {
        user = optionalUser.get();
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(user.getEmail());
        loginRequest.setPassword(request.getCurrentPassword());

        // Will throw exception if login details are invalid
        authControllerService.authenticateUser(loginRequest);

        boolean isPasswordValidation = ValidationUtil.isValidPassword(request.getNewPassword());

        if (isPasswordValidation) {
          user.setPassword(encoder.encode(request.getNewPassword()));
          userRepository.save(user);
          asyncService.notifyUserPasswordChange(user,request.getNewPassword());
          return ResponseEntity.ok(new MessageResponse(true, "Password updated successfully"));
        } else {
          logger.info("Invalid password, please provide a strong password");
          return ResponseEntity.badRequest()
              .body(
                  new MessageResponse(false, "Invalid password, please provide a strong password"));
        }

      } else {
        logger.info("Invalid user ID: {}", request.getUserId());
        return ResponseEntity.badRequest().body(new MessageResponse(false, "Invalid user ID"));
      }

    } catch (LearnBridgeException e) {
      logger.error("Error while updating login details: ", e);
      return ResponseEntity.badRequest()
          .body(
              new MessageResponse(
                  false,
                  "We are unable to update your password, please verify that your current password is correct and try again"));
    }
  }

  @Override
  public ResponseEntity<Object> findUserById(String userId) {

    Optional<User> optionalUser = getUserOptional(userId);
    User user;
    if (optionalUser.isPresent()) {
      user = optionalUser.get();
      return ResponseEntity.ok(LearnBridgeUtil.getUserInfoResponse(user));
    } else {
      logger.info("Invalid user ID: {}", userId);
      return ResponseEntity.badRequest().body(new MessageResponse(false, "Invalid user ID"));
    }
  }

  @Override
  public ResponseEntity<Object> getFundingData(String userId) {

    // TODO Mock data for now, r
    Optional<User> optionalUser = getUserOptional(userId);
    User user;
    if (optionalUser.isPresent()) {
      user = optionalUser.get();

      List<FundingDetailsDto> fundingList = new ArrayList<>();
      FundingDetailsDto nsfas = new FundingDetailsDto();
      nsfas.setId("1");
      nsfas.setName("NSFAS (National Student Financial Aid Scheme)");
      nsfas.setType("Government");
      nsfas.setAmount("Full Coverage");
      nsfas.setDeadline("30 November 2024");
      nsfas.setDescription(
          "Comprehensive financial aid covering tuition, accommodation, transport, and living allowances for students from poor and working-class families.");
      nsfas.setRequirements(
          Arrays.asList(
              "South African citizen",
              "Combined family income under R350,000",
              "SASSA grant recipient qualifies automatically",
              "Must be accepted at a public university or TVET college"));
      nsfas.setWebsite("https://www.nsfas.org.za");
      nsfas.setFields(Arrays.asList("All fields of study"));
      nsfas.setCoverage(
          Arrays.asList(
              "Tuition fees",
              "Accommodation",
              "Transport",
              "Living allowance",
              "Learning materials"));
      nsfas.setColor("bg-blue-500");
      nsfas.setCriteria("Need-based");

      FundingDetailsDto funza = new FundingDetailsDto();
      funza.setId("2");
      funza.setName("Funza Lushaka Bursary");
      funza.setType("Government");
      funza.setAmount("Full Coverage");
      funza.setDeadline("31 January 2025");
      funza.setDescription(
          "Provides full-cost funding for students studying to become teachers in priority subjects.");
      funza.setRequirements(
          Arrays.asList(
              "South African citizen",
              "Accepted into a B.Ed. or PGCE program at a recognized higher education institution",
              "Commitment to teach in public schools after graduation"));
      funza.setWebsite("https://www.funzalushaka.doe.gov.za");
      funza.setFields(
          Arrays.asList(
              "Education (priority subjects: Maths, Science, Technology, African Languages)"));
      funza.setCoverage(
          Arrays.asList("Tuition fees", "Accommodation", "Living allowance", "Books"));
      funza.setColor("bg-green-500");
      funza.setCriteria("Need-based");

      FundingDetailsDto sasol = new FundingDetailsDto();
      sasol.setId("3");
      sasol.setName("Sasol Bursary Programme");
      sasol.setType("Corporate");
      sasol.setAmount("Full Coverage");
      sasol.setDeadline("30 April 2025");
      sasol.setDescription(
          "Funds studies in engineering, science, and technology fields with opportunities for vacation work and graduate programs.");
      sasol.setRequirements(
          Arrays.asList(
              "Excellent academic performance (minimum 70% average in Mathematics and Science)",
              "South African citizen",
              "Studying towards engineering, science, or technical degrees"));
      sasol.setWebsite("https://www.sasolbursaries.com");
      sasol.setFields(Arrays.asList("Engineering", "Science", "Technology"));
      sasol.setCoverage(
          Arrays.asList(
              "Tuition fees", "Accommodation", "Books", "Living allowance", "Vacation work"));
      sasol.setColor("bg-yellow-500");
      sasol.setCriteria("Merit-based");

      FundingDetailsDto allanGray = new FundingDetailsDto();
      allanGray.setId("4");
      allanGray.setName("Allan Gray Orbis Fellowship");
      allanGray.setType("Corporate/Foundation");
      allanGray.setAmount("Full Coverage + Mentorship");
      allanGray.setDeadline("30 September 2025");
      allanGray.setDescription(
          "Provides full funding for university plus mentorship and entrepreneurial leadership development.");
      allanGray.setRequirements(
          Arrays.asList(
              "Strong academic results (minimum 70% average)",
              "Leadership potential", "South African citizen"));
      allanGray.setWebsite("https://www.allangrayorbis.org");
      allanGray.setFields(Arrays.asList("Commerce", "Science", "Engineering", "Humanities"));
      allanGray.setCoverage(
          Arrays.asList(
              "Tuition fees", "Accommodation", "Books", "Living allowance", "Mentorship"));
      allanGray.setColor("bg-purple-500");
      allanGray.setCriteria("Merit-based");

      FundingDetailsDto eskom = new FundingDetailsDto();
      eskom.setId("5");
      eskom.setName("Eskom Bursary Programme");
      eskom.setType("Corporate");
      eskom.setAmount("Full / Partial Coverage");
      eskom.setDeadline("31 July 2025");
      eskom.setDescription(
          "Supports studies in engineering, accounting, IT, and other scarce skills fields.");
      eskom.setRequirements(
          Arrays.asList(
              "Strong academic performance (minimum 65-70%)",
              "South African citizen", "Preference for financially needy students"));
      eskom.setWebsite("https://www.eskom.co.za/careers/bursaries");
      eskom.setFields(Arrays.asList("Engineering", "Accounting", "IT", "Science"));
      eskom.setCoverage(Arrays.asList("Tuition fees", "Accommodation", "Books", "Allowance"));
      eskom.setColor("bg-red-500");
      eskom.setCriteria("Mixed");

      FundingDetailsDto investec = new FundingDetailsDto();
      investec.setId("6");
      investec.setName("Investec Bursary");
      investec.setType("Corporate");
      investec.setAmount("Full Coverage");
      investec.setDeadline("30 September 2025");
      investec.setDescription(
          "Funds high-potential students in commerce, engineering, IT, and related fields.");
      investec.setRequirements(
          Arrays.asList(
              "Excellent academic results (minimum 70% average)",
              "South African citizen", "Preference to students with financial need"));
      investec.setWebsite("https://www.investecbursaries.co.za");
      investec.setFields(Arrays.asList("Commerce", "Engineering", "IT", "Business"));
      investec.setCoverage(Arrays.asList("Tuition fees", "Accommodation", "Books", "Allowance"));
      investec.setColor("bg-indigo-500");
      investec.setCriteria("Mixed");

      FundingDetailsDto bmw = new FundingDetailsDto();
      bmw.setId("7");
      bmw.setName("BMW Bursary Programme");
      bmw.setType("Corporate");
      bmw.setAmount("Full / Partial Coverage");
      bmw.setDeadline("31 August 2025");
      bmw.setDescription(
          "Provides funding for engineering, IT, and business studies, often with internship opportunities at BMW SA.");
      bmw.setRequirements(
          Arrays.asList(
              "Strong academic performance",
              "South African citizen",
              "Interest in automotive and technology fields"));
      bmw.setWebsite("https://www.bmwgroup.com/bursaries");
      bmw.setFields(Arrays.asList("Engineering", "IT", "Business"));
      bmw.setCoverage(
          Arrays.asList("Tuition fees", "Accommodation", "Books", "Allowance", "Internship"));
      bmw.setColor("bg-gray-500");
      bmw.setCriteria("Merit-based");

      fundingList.add(nsfas);
      fundingList.add(funza);
      fundingList.add(sasol);
      fundingList.add(allanGray);
      fundingList.add(eskom);
      fundingList.add(investec);
      fundingList.add(bmw);

      return ResponseEntity.ok(new FundingResponse(fundingList));

    } else {
      logger.info("Invalid user ID: {}", userId);
      return ResponseEntity.badRequest().body(new MessageResponse(false, "Invalid user ID"));
    }
  }

  @Override
  public ResponseEntity<Object> getDashboardData(String userId) {

    Optional<User> optionalUser = getUserOptional(userId);
    User user;
    if (optionalUser.isPresent()) {
      user = optionalUser.get();

      // TODO Mock data for now, replace with RecommendationService later
      List<CourseDto> courses =
          List.of(
              new CourseDto(
                  "1",
                  "Computer Science",
                  "Learn programming, algorithms, and software development",
                  List.of("Mathematics", "Physical Sciences"),
                  "University of the Witwatersrand",
                  "4 years",
                  "Bachelor of Science"),
              new CourseDto(
                  "2",
                  "Mechanical Engineering",
                  "Design and build mechanical systems",
                  List.of("Mathematics", "Physical Sciences", "Engineering Graphics and Design"),
                  "University of Cape Town",
                  "4 years",
                  "Bachelor of Engineering"),
              new CourseDto(
                  "3",
                  "Business Administration",
                  "Learn business management and entrepreneurship",
                  List.of("Mathematics", "Business Studies", "Accounting"),
                  "Stellenbosch University",
                  "3 years",
                  "Bachelor of Commerce"));

      List<JobTrendDto> jobs =
          List.of(
              new JobTrendDto("Software Developer", "High", "R35,000 - R85,000"),
              new JobTrendDto("Data Scientist", "Very High", "R40,000 - R100,000"),
              new JobTrendDto("Digital Marketing Specialist", "High", "R25,000 - R55,000"));

      List<FundingDto> fundings =
          List.of(
              new FundingDto("NSFAS Bursary", "Government", "Full Coverage"),
              new FundingDto("Sasol Bursary", "Corporate", "R80,000/year"),
              new FundingDto("Allan Gray Orbis Foundation", "Private", "Full Coverage"));

      return ResponseEntity.ok(
          new DashboardResponse(
              user.getSubjects().size(),
              user.getInterests().size(),
              courses.size(),
              courses,
              jobs,
              fundings));

    } else {
      logger.info("Invalid user ID: {}", userId);
      return ResponseEntity.badRequest().body(new MessageResponse(false, "Invalid user ID"));
    }
  }

  @Override
  public ResponseEntity<Object> getJobMarket(String userId) {

    // TODO Mock data for now, r
    Optional<User> optionalUser = getUserOptional(userId);
    User user;
    if (optionalUser.isPresent()) {
      user = optionalUser.get();
      return ResponseEntity.ok(getJobMarketData());

    } else {
      logger.info("Invalid user ID: {}", userId);
      return ResponseEntity.badRequest().body(new MessageResponse(false, "Invalid user ID"));
    }
  }

  public JobMarketResponse getJobMarketData() {
    JobDto dataScientist = new JobDto();
    dataScientist.setTitle("Data Scientist");
    dataScientist.setIndustry("Technology");
    dataScientist.setDemand("Very High");
    dataScientist.setSalary("R45,000 - R120,000");
    dataScientist.setGrowth("+18% annually");
    dataScientist.setDescription("Analyze complex data to help businesses make informed decisions");
    dataScientist.setRequirements(
        Arrays.asList("Statistics/Mathematics", "Programming (Python/R)", "Machine Learning"));
    dataScientist.setCompanies(
        Arrays.asList("Standard Bank", "Discovery", "Shoprite Holdings", "MTN"));
    dataScientist.setEducation("Bachelor's in Statistics, Computer Science, or Mathematics");
    dataScientist.setHotspots(Arrays.asList("Gauteng", "Western Cape"));

    JobDto softwareDev = new JobDto();
    softwareDev.setTitle("Software Developer");
    softwareDev.setIndustry("Technology");
    softwareDev.setDemand("Very High");
    softwareDev.setSalary("R35,000 - R95,000");
    softwareDev.setGrowth("+15% annually");
    softwareDev.setDescription("Design, develop, and maintain software applications and systems");
    softwareDev.setRequirements(
        Arrays.asList("Programming Languages", "Problem Solving", "System Design"));
    softwareDev.setCompanies(
        Arrays.asList("Capitec Bank", "Takealot", "Mr Price Group", "Nedbank"));
    softwareDev.setEducation("Bachelor's in Computer Science or Information Technology");
    softwareDev.setHotspots(Arrays.asList("Gauteng", "Western Cape", "KwaZulu-Natal"));

    // Group by category
    Map<String, List<JobDto>> jobsByCategory = new HashMap<>();
    jobsByCategory.put("trending", Arrays.asList(dataScientist, softwareDev));
    jobsByCategory.put("tech", Arrays.asList(dataScientist, softwareDev));
    // add other categories as needed...

    // Market insights
    SectorDto tech = new SectorDto();
    tech.setName("Technology");
    tech.setGrowth("+15%");
    tech.setJobs("12,400 new positions");

    SectorDto healthcare = new SectorDto();
    healthcare.setName("Healthcare");
    healthcare.setGrowth("+12%");
    healthcare.setJobs("8,200 new positions");

    List<SectorDto> fastestGrowingSectors = Arrays.asList(tech, healthcare);

    List<String> inDemandSkills =
        Arrays.asList(
            "Digital Literacy",
            "Data Analysis",
            "Problem Solving",
            "Communication",
            "Programming",
            "Project Management");

    MarketInsightsDto insights = new MarketInsightsDto();
    insights.setFastestGrowingSectors(fastestGrowingSectors);
    insights.setInDemandSkills(inDemandSkills);

    JobMarketResponse response = new JobMarketResponse();
    response.setJobsByCategory(jobsByCategory);
    response.setMarketInsights(insights);

    return response;
  }

  private User getUser(UpdateUserRequest request) throws LearnBridgeException {
    Optional<User> optionalUser = getUserOptional(request.getUserId());
    User user;
    if (optionalUser.isPresent()) {
      user = optionalUser.get();
      user.setName(request.getName());
      user.setSurname(request.getSurname());
      user.setEmail(request.getEmail());
      user.setPhoneNumber(request.getPhoneNumber());
      if (user.getLearnerNumber() == null) {
        user.setLearnerNumber(
            LearnBridgeUtil.generateLearnerNumber(
                request.getName(),
                request.getSurname(),
                request.getPhoneNumber(),
                user.getCreatedDate()));
      }
    } else {
      user = new User();
      user.setName(request.getName());
      user.setSurname(request.getSurname());
      user.setEmail(request.getEmail());
      user.setPhoneNumber(request.getPhoneNumber());
      user.setPassword(encoder.encode(LearnBridgeUtil.generatePassword()));
    }
    return user;
  }

  private Optional<User> getUserOptional(String id) {
    if (id != null) {
      return userRepository.findById(id);
    } else {
      return Optional.empty();
    }
  }
}
