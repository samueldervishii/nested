package com.nested.server.repository;

import com.nested.server.model.Vote;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends MongoRepository<Vote, String> {

    Optional<Vote> findByUserIdAndTargetIdAndTargetType(String userId, String targetId, Vote.VoteTargetType targetType);

    List<Vote> findByUserIdAndTargetIdIn(String userId, List<String> targetIds);

    /**
     * Atomic vote type update
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'voteType': ?1 } }")
    void updateVoteType(String voteId, Vote.VoteType voteType);
}
