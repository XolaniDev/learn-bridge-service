package co.za.learn.bridge.repository;

import co.za.learn.bridge.model.entity.FundingDetails;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FundingRepository extends MongoRepository<FundingDetails, String> {}
