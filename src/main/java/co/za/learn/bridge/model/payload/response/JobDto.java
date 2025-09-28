package co.za.learn.bridge.model.payload.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobDto {
    private String title;
    private String industry;
    private String demand;
    private String salary;
    private String growth;
    private String description;
    private List<String> requirements;
    private List<String> companies;
    private String education;
    private List<String> hotspots;
}