package co.za.learn.bridge.model.payload.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FundingDetailsDto {
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
    private String criteria; // <-- (Merit-based / Need-based / Mixed)
}
