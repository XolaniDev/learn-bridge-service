package co.za.learn.bridge.repository;

import co.za.learn.bridge.model.entity.FundingDetails;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class FundingRepository {
    private final Map<String, FundingDetails> store = new ConcurrentHashMap<>();

    public void save(FundingDetails dto) {
        store.put(dto.getId(), dto);
    }

    public Optional<FundingDetails> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<FundingDetails> findAll() {
        return new ArrayList<>(store.values());
    }

    public boolean existsById(String id) {
        return store.containsKey(id);
    }

    public void clear() {
        store.clear();
    }
}
