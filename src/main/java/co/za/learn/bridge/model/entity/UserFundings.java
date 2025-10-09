// FundingDetails.java
package co.za.learn.bridge.model.entity;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "user_fundings")
public class UserFundings {
    @Id
    private String id;
    @NotBlank
    private String userId;
    private String name;
    private String type;
    private String amount;
    private String deadline;
    private String description;
    private List<String> requirements;
    private String website;
    private List<String> fields;
    private List<String> keywords;
    private List<String> coverage;
    private String color;
    private String criteria;

}
