package com.nested.server.repository;

import com.nested.server.model.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {

    List<Comment> findByPostIdOrderByCreatedAtDesc(String postId);

    /**
     * Atomic vote count increment using MongoDB's $inc operator
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'voteCount': ?1 } }")
    void incrementVoteCount(String commentId, int delta);
}
