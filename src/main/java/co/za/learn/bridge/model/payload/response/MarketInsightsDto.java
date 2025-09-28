package co.za.learn.bridge.model.payload.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MarketInsightsDto {
    private List<SectorDto> fastestGrowingSectors;
    private List<String> inDemandSkills;
}