// JobTrendDto.java
package co.za.learn.bridge.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobTrendDto {
    private String title;
    private String demand;
    private String salary;
}
