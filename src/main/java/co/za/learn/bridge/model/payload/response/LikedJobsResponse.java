package co.za.learn.bridge.model.payload.response;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikedJobsResponse {
    private List<JobDto> likedJobList;
}