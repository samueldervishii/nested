package com.nested.repository;

import com.nested.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {

    List<Comment> findByPostIdOrderByCreatedAtDesc(String postId);

    List<Comment> findByPostIdAndParentCommentIdIsNullOrderByVoteCountDesc(String postId);

    List<Comment> findByParentCommentIdOrderByVoteCountDesc(String parentCommentId);

    Page<Comment> findByAuthorId(String authorId, Pageable pageable);

    int countByPostId(String postId);
}
