package com.nested.server.repository;

import com.nested.server.model.Ban;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BanRepository extends MongoRepository<Ban, String> {

    Optional<Ban> findBySubIdAndUserId(String subId, String userId);

    List<Ban> findBySubId(String subId);

    Page<Ban> findBySubId(String subId, Pageable pageable);

    List<Ban> findByUserId(String userId);

    boolean existsBySubIdAndUserId(String subId, String userId);

    void deleteBySubIdAndUserId(String subId, String userId);
}
