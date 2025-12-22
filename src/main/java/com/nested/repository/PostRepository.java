package com.nested.repository;

import com.nested.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {

    Page<Post> findBysubnestedId(String subnestedId, Pageable pageable);

    Page<Post> findBysubnestedName(String subnestedName, Pageable pageable);

    Page<Post> findByAuthorId(String authorId, Pageable pageable);

    Page<Post> findBysubnestedIdIn(List<String> subnestedIds, Pageable pageable);

    Page<Post> findByCreatedAtAfter(Instant since, Pageable pageable);

    List<Post> findTop100ByOrderByVoteCountDesc();

    List<Post> findTop100ByOrderByCreatedAtDesc();

    List<Post> findTop100ByCreatedAtAfterOrderByVoteCountDesc(Instant since);

    // Search by title or content containing keyword (case insensitive)
    Page<Post> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
            String title, String content, Pageable pageable);

    // Find posts by IDs (for saved posts)
    List<Post> findByIdIn(Set<String> ids);
}
