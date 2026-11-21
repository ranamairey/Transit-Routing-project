package com.example.demo.transit.repository;

import com.example.demo.transit.entity.PushSubscriptionRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends MongoRepository<PushSubscriptionRecord, String> {
    Optional<PushSubscriptionRecord> findByEndpoint(String endpoint);

    List<PushSubscriptionRecord> findByUserEmailIn(Collection<String> emails);

    void deleteByEndpoint(String endpoint);
}
