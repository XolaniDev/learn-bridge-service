package co.za.learn.bridge.model.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SectorDto {
    private String name;
    private String growth;
    private String jobs;
}