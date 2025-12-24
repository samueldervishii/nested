package com.nested.server.service;

import com.nested.server.dto.CommentRequest;
import com.nested.server.dto.CommentResponse;
import com.nested.server.exception.BadRequestException;
import com.nested.server.exception.ResourceNotFoundException;
import com.nested.server.exception.UnauthorizedException;
import com.nested.server.model.Comment;
import com.nested.server.model.User;
import com.nested.server.model.Vote;
import com.nested.server.repository.CommentRepository;
import com.nested.server.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final int MAX_COMMENT_DEPTH = 10;
    private static final int MAX_COMMENT_LENGTH = 10000;

    private final CommentRepository commentRepository;
    private final PostService postService;
    private final VoteRepository voteRepository;
    private final UserService userService;

    @Transactional
    public CommentResponse createComment(CommentRequest request, User author) {
        // Validate comment content
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BadRequestException("Comment content cannot be empty");
        }
        if (request.getContent().length() > MAX_COMMENT_LENGTH) {
            throw new BadRequestException("Comment is too long (max " + MAX_COMMENT_LENGTH + " characters)");
        }

        int depth = 0;
        if (request.getParentCommentId() != null) {
            Comment parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", request.getParentCommentId()));
            depth = parent.getDepth() + 1;

            // Enforce maximum depth limit to prevent abuse
            if (depth > MAX_COMMENT_DEPTH) {
                throw new BadRequestException("Maximum comment depth reached. Please reply to a higher-level comment.");
            }
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .authorId(author.getId())
                .authorName(author.getUsername())
                .postId(request.getPostId())
                .parentCommentId(request.getParentCommentId())
                .depth(depth)
                .voteCount(1)
                .createdAt(Instant.now())
                .build();

        comment = commentRepository.save(comment);

        // Update post comment count
        postService.updateCommentCount(request.getPostId(), 1);

        // Update author karma
        userService.updateKarma(author.getId(), 1);

        // Auto-upvote own comment
        Vote vote = Vote.builder()
                .userId(author.getId())
                .targetId(comment.getId())
                .targetType(Vote.VoteTargetType.COMMENT)
                .voteType(Vote.VoteType.UPVOTE)
                .build();
        voteRepository.save(vote);

        return mapToResponse(comment, 1);
    }

    /**
     * Original method - loads all comments (kept for backwards compatibility)
     */
    public List<CommentResponse> getCommentsByPost(String postId, User currentUser) {
        return getCommentsByPostPaginated(postId, currentUser, 0, 50, true);
    }

    /**
     * Optimized paginated comment loading:
     * - Only loads root comments initially (paginated)
     * - Child comments can be loaded on-demand via getReplies()
     * - Reduces memory usage for posts with many comments
     */
    public List<CommentResponse> getCommentsByPostPaginated(String postId, User currentUser,
                                                             int page, int size, boolean includeReplies) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "voteCount"));
        List<Comment> rootComments = commentRepository.findRootCommentsByPostId(postId, pageable);

        if (rootComments.isEmpty()) {
            return Collections.emptyList();
        }

        // Get all comment IDs for vote lookup
        List<String> commentIds = new ArrayList<>();
        rootComments.forEach(c -> commentIds.add(c.getId()));

        Map<String, Integer> userVotes = new HashMap<>();
        Map<String, List<Comment>> childrenMap = new HashMap<>();

        if (includeReplies) {
            // Load all child comments for these root comments
            List<Comment> allComments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId);

            for (Comment comment : allComments) {
                commentIds.add(comment.getId());
                if (comment.getParentCommentId() != null) {
                    childrenMap.computeIfAbsent(comment.getParentCommentId(), k -> new ArrayList<>()).add(comment);
                }
            }
        }

        // Batch load user votes
        if (currentUser != null && !commentIds.isEmpty()) {
            List<Vote> votes = voteRepository.findByUserIdAndTargetIdIn(currentUser.getId(), commentIds);
            votes.forEach(v -> userVotes.put(v.getTargetId(), v.getVoteType().getValue()));
        }

        return rootComments.stream()
                .map(comment -> includeReplies
                    ? buildCommentTree(comment, childrenMap, userVotes)
                    : buildCommentWithReplyCount(comment, userVotes))
                .collect(Collectors.toList());
    }

    /**
     * Lazy load replies for a specific comment
     */
    public List<CommentResponse> getReplies(String parentCommentId, User currentUser) {
        List<Comment> replies = commentRepository.findByParentCommentIdOrderByVoteCountDesc(parentCommentId);

        if (replies.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Integer> userVotes = new HashMap<>();
        if (currentUser != null) {
            List<String> replyIds = replies.stream().map(Comment::getId).collect(Collectors.toList());
            List<Vote> votes = voteRepository.findByUserIdAndTargetIdIn(currentUser.getId(), replyIds);
            votes.forEach(v -> userVotes.put(v.getTargetId(), v.getVoteType().getValue()));
        }

        return replies.stream()
                .map(comment -> mapToResponse(comment, userVotes.get(comment.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Get total count of root comments (for pagination info)
     */
    public long getRootCommentCount(String postId) {
        return commentRepository.countByPostIdAndParentCommentIdIsNull(postId);
    }

    private CommentResponse buildCommentWithReplyCount(Comment comment, Map<String, Integer> userVotes) {
        CommentResponse response = mapToResponse(comment, userVotes.get(comment.getId()));
        // Set empty replies list - client can load on demand
        response.setReplies(Collections.emptyList());
        return response;
    }

    private CommentResponse buildCommentTree(Comment comment, Map<String, List<Comment>> childrenMap, Map<String, Integer> userVotes) {
        CommentResponse response = mapToResponse(comment, userVotes.get(comment.getId()));

        List<Comment> children = childrenMap.getOrDefault(comment.getId(), Collections.emptyList());
        children.sort((a, b) -> Integer.compare(b.getVoteCount(), a.getVoteCount()));

        List<CommentResponse> replies = children.stream()
                .map(child -> buildCommentTree(child, childrenMap, userVotes))
                .collect(Collectors.toList());

        response.setReplies(replies);
        return response;
    }

    /**
     * Atomic vote count update using MongoDB $inc operator
     */
    public void updateVoteCount(String commentId, int delta) {
        commentRepository.incrementVoteCount(commentId, delta);
    }

    public Optional<Comment> findById(String id) {
        return commentRepository.findById(id);
    }

    public CommentResponse updateComment(String commentId, String content, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (!comment.getAuthorId().equals(user.getId())) {
            throw new UnauthorizedException("You can only edit your own comments");
        }

        if (comment.isDeleted()) {
            throw new BadRequestException("Cannot edit deleted comment");
        }

        comment.setContent(content);
        comment = commentRepository.save(comment);

        return mapToResponse(comment, null);
    }

    public void deleteComment(String commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (!comment.getAuthorId().equals(user.getId())) {
            throw new UnauthorizedException("You can only delete your own comments");
        }

        // Soft delete - keep the comment but mark it as deleted
        comment.setDeleted(true);
        comment.setContent("[deleted]");
        commentRepository.save(comment);

        // Update post comment count
        postService.updateCommentCount(comment.getPostId(), -1);
    }

    public CommentResponse mapToResponse(Comment comment, Integer userVote) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.isDeleted() ? "[deleted]" : comment.getContent())
                .authorId(comment.getAuthorId())
                .authorUsername(comment.isDeleted() ? "[deleted]" : comment.getAuthorName())
                .postId(comment.getPostId())
                .parentCommentId(comment.getParentCommentId())
                .voteCount(comment.getVoteCount())
                .depth(comment.getDepth())
                .createdAt(formatDate(comment.getCreatedAt()))
                .timeAgo(getTimeAgo(comment.getCreatedAt()))
                .deleted(comment.isDeleted())
                .userVote(userVote)
                .build();
    }

    private String formatDate(Instant instant) {
        return DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    private String getTimeAgo(Instant instant) {
        Duration duration = Duration.between(instant, Instant.now());

        if (duration.toMinutes() < 1) {
            return "just now";
        } else if (duration.toMinutes() < 60) {
            long mins = duration.toMinutes();
            return mins + (mins == 1 ? " minute ago" : " minutes ago");
        } else if (duration.toHours() < 24) {
            long hours = duration.toHours();
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (duration.toDays() < 30) {
            long days = duration.toDays();
            return days + (days == 1 ? " day ago" : " days ago");
        } else {
            long years = duration.toDays() / 365;
            return years + (years == 1 ? " year ago" : " years ago");
        }
    }
}
