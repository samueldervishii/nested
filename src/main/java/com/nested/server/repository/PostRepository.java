package com.nested.server.repository;

import com.nested.server.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {

    Page<Post> findBySubName(String subName, Pageable pageable);

    Page<Post> findByAuthorId(String authorId, Pageable pageable);

    Page<Post> findBySubIdIn(List<String> subIds, Pageable pageable);

    List<Post> findTop100ByOrderByVoteCountDesc();

    List<Post> findTop100ByOrderByCreatedAtDesc();

    List<Post> findTop100ByCreatedAtAfterOrderByVoteCountDesc(Instant since);

    // Text search using MongoDB text index (more efficient than regex)
    @Query("{ '$text': { '$search': ?0 } }")
    Page<Post> searchByText(String searchQuery, Pageable pageable);

    // Fallback: Search by title or content containing keyword (case-insensitive)
    Page<Post> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
            String title, String content, Pageable pageable);

    // Find posts by IDs (for saved posts)
    List<Post> findByIdIn(Set<String> ids);

    /**
     * Atomic vote count increment using MongoDB's $inc operator
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'voteCount': ?1 } }")
    void incrementVoteCount(String postId, int delta);

    /**
     * Atomic comment count increment using MongoDB's $inc operator
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'commentCount': ?1 } }")
    void incrementCommentCount(String postId, int delta);

    /**
     * Projection query for vote operations - only fetches authorId and voteCount
     */
    @Query(value = "{ '_id': ?0 }", fields = "{ 'authorId': 1, 'voteCount': 1 }")
    Optional<Post> findAuthorIdAndVoteCountById(String postId);

    /**
     * Lightweight projection for post lists - excludes large fields like content and imageUrls
     */
    @Query(value = "{}", fields = "{ 'content': 0, 'imageUrls': 0 }")
    List<Post> findAllProjectedForList();
}
