// DashboardResponse.java
package co.za.learn.bridge.model.payload.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardResponse {
    private  int subjects;
    private  int interests;
    private  int matches;
    private List<CourseDto> recommendedCourses;
    private List<JobTrendDto> jobTrends;
    private List<FundingDto> fundingOpportunities;

}
