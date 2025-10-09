package co.za.learn.bridge.model.payload.response;

import co.za.learn.bridge.model.entity.FundingDetails;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FundingResponse {
  private List<FundingDetails> fundingList;
}
