// FundingDetails.java
package co.za.learn.bridge.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "funding_details")
public class FundingDetails {
    @Id
    private String id;
    private String name;
    private String type;
    private String amount;
    private String deadline;
    private String description;
    private List<String> requirements;
    private String website;
    private List<String> fields;
    private List<String> coverage;
    private String color;
    private String criteria;

}
