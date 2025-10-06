package co.za.learn.bridge.model.entity;

import co.za.learn.bridge.model.payload.response.CourseDto;
import co.za.learn.bridge.model.payload.response.FundingDto;
import co.za.learn.bridge.model.payload.response.JobTrendDto;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@Document(collection = "recommendations")
public class Recommendations {
    @Id
    private String id;
    @NotBlank
    private String userId;
    private List<CourseDto> recommendedCourses;
    private List<JobTrendDto> jobTrends;
    private List<FundingDto> fundingOpportunities;
    @NotBlank
    private Date createdDate;

}
