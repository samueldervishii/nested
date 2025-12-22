package com.nested.repository;

import com.nested.model.Vote;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends MongoRepository<Vote, String> {

    Optional<Vote> findByUserIdAndTargetIdAndTargetType(String userId, String targetId, Vote.VoteTargetType targetType);

    List<Vote> findByUserIdAndTargetIdIn(String userId, List<String> targetIds);

    void deleteByUserIdAndTargetIdAndTargetType(String userId, String targetId, Vote.VoteTargetType targetType);
}
