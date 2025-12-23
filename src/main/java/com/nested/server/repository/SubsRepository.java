package com.nested.server.repository;

import com.nested.server.model.Subs;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubsRepository extends MongoRepository<Subs, String> {

    Optional<Subs> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Subs> findByNameContainingIgnoreCase(String query);

    List<Subs> findTop10ByOrderBySubscriberCountDesc();

    List<Subs> findByIdIn(List<String> ids);

    // Find subs where user is a moderator
    List<Subs> findByModeratorIdsContaining(String moderatorId);

    // Find subs created by user
    List<Subs> findByCreatorId(String creatorId);
}
