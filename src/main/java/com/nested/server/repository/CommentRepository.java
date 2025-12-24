package com.nested.server.repository;

import com.nested.server.model.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {

    List<Comment> findByPostIdOrderByCreatedAtDesc(String postId);

    /**
     * Atomic vote count increment using MongoDB's $inc operator
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'voteCount': ?1 } }")
    void incrementVoteCount(String commentId, int delta);

    /**
     * Projection query for vote operations - only fetches authorId and voteCount
     */
    @Query(value = "{ '_id': ?0 }", fields = "{ 'authorId': 1, 'voteCount': 1 }")
    Optional<Comment> findAuthorIdAndVoteCountById(String commentId);

    /**
     * Paginated root comments (no parent) - for lazy loading
     */
    @Query("{ 'postId': ?0, 'parentCommentId': null }")
    List<Comment> findRootCommentsByPostId(String postId, org.springframework.data.domain.Pageable pageable);

    /**
     * Count root comments for pagination info
     */
    long countByPostIdAndParentCommentIdIsNull(String postId);

    /**
     * Find child comments for a specific parent (lazy loading replies)
     */
    List<Comment> findByParentCommentIdOrderByVoteCountDesc(String parentCommentId);
}
