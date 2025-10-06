// CourseDto.java
package co.za.learn.bridge.model.payload.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseDto {
    private String id;
    private String name;
    private String description;
    private List<String> requiredSubjects;
    private String university;
    private String duration;
    private String qualification;
}
