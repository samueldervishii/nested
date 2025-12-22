package com.nested.service;

import com.nested.dto.CommentRequest;
import com.nested.dto.CommentResponse;
import com.nested.exception.BadRequestException;
import com.nested.exception.ResourceNotFoundException;
import com.nested.exception.UnauthorizedException;
import com.nested.model.Comment;
import com.nested.model.User;
import com.nested.model.Vote;
import com.nested.repository.CommentRepository;
import com.nested.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;
    private final VoteRepository voteRepository;
    private final UserService userService;

    public CommentResponse createComment(CommentRequest request, User author) {
        int depth = 0;
        if (request.getParentCommentId() != null) {
            Comment parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", request.getParentCommentId()));
            depth = parent.getDepth() + 1;
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .authorId(author.getId())
                .authorUsername(author.getUsername())
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

    public List<CommentResponse> getCommentsByPost(String postId, User currentUser) {
        List<Comment> allComments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId);

        Map<String, Integer> userVotes = new HashMap<>();
        if (currentUser != null) {
            List<String> commentIds = allComments.stream().map(Comment::getId).collect(Collectors.toList());
            List<Vote> votes = voteRepository.findByUserIdAndTargetIdIn(currentUser.getId(), commentIds);
            votes.forEach(v -> userVotes.put(v.getTargetId(), v.getVoteType().getValue()));
        }

        // Build comment tree
        Map<String, List<Comment>> childrenMap = new HashMap<>();
        List<Comment> rootComments = new ArrayList<>();

        for (Comment comment : allComments) {
            if (comment.getParentCommentId() == null) {
                rootComments.add(comment);
            } else {
                childrenMap.computeIfAbsent(comment.getParentCommentId(), k -> new ArrayList<>()).add(comment);
            }
        }

        // Sort root comments by vote count
        rootComments.sort((a, b) -> Integer.compare(b.getVoteCount(), a.getVoteCount()));

        return rootComments.stream()
                .map(comment -> buildCommentTree(comment, childrenMap, userVotes))
                .collect(Collectors.toList());
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

    public void updateVoteCount(String commentId, int delta) {
        commentRepository.findById(commentId).ifPresent(comment -> {
            comment.setVoteCount(comment.getVoteCount() + delta);
            commentRepository.save(comment);
        });
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
                .authorUsername(comment.isDeleted() ? "[deleted]" : comment.getAuthorUsername())
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
