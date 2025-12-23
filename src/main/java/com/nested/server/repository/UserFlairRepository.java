package com.nested.server.repository;

import com.nested.server.model.UserFlair;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFlairRepository extends MongoRepository<UserFlair, String> {

    Optional<UserFlair> findBySubIdAndUserId(String subId, String userId);

    List<UserFlair> findBySubId(String subId);

    List<UserFlair> findByUserId(String userId);

    boolean existsBySubIdAndUserId(String subId, String userId);

    void deleteBySubIdAndUserId(String subId, String userId);
}
