// FundingDto.java
package co.za.learn.bridge.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class FundingDto {
    private String name;
    private String type;
    private String amount;
}
