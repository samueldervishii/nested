package com.nested.server.service;


import com.nested.server.dto.VoteRequest;
import com.nested.server.model.Comment;
import com.nested.server.model.Post;
import com.nested.server.model.User;
import com.nested.server.model.Vote;
import com.nested.server.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final PostService postService;
    private final CommentService commentService;
    private final UserService userService;

    @Transactional
    public int vote(VoteRequest request, User user) {
        Optional<Vote> existingVote = voteRepository.findByUserIdAndTargetIdAndTargetType(
                user.getId(), request.getTargetId(), request.getTargetType());

        String authorId = getAuthorId(request.getTargetId(), request.getTargetType());
        int voteChange = 0;

        if (existingVote.isPresent()) {
            Vote vote = existingVote.get();

            if (vote.getVoteType() == request.getVoteType()) {
                // Remove vote (toggle off)
                voteRepository.delete(vote);
                voteChange = -vote.getVoteType().getValue();
            } else {
                // Change vote direction
                voteChange = request.getVoteType().getValue() - vote.getVoteType().getValue();
                vote.setVoteType(request.getVoteType());
                voteRepository.save(vote);
            }
        } else {
            // New vote
            Vote vote = Vote.builder()
                    .userId(user.getId())
                    .targetId(request.getTargetId())
                    .targetType(request.getTargetType())
                    .voteType(request.getVoteType())
                    .build();
            voteRepository.save(vote);
            voteChange = request.getVoteType().getValue();
        }

        // Update vote count on target
        updateTargetVoteCount(request.getTargetId(), request.getTargetType(), voteChange);

        // Update author karma
        if (authorId != null && !authorId.equals(user.getId())) {
            userService.updateKarma(authorId, voteChange);
        }

        return getTargetVoteCount(request.getTargetId(), request.getTargetType());
    }

    private String getAuthorId(String targetId, Vote.VoteTargetType targetType) {
        if (targetType == Vote.VoteTargetType.POST) {
            return postService.findById(targetId).map(Post::getAuthorId).orElse(null);
        } else {
            return commentService.findById(targetId).map(Comment::getAuthorId).orElse(null);
        }
    }

    private void updateTargetVoteCount(String targetId, Vote.VoteTargetType targetType, int delta) {
        if (targetType == Vote.VoteTargetType.POST) {
            postService.updateVoteCount(targetId, delta);
        } else {
            commentService.updateVoteCount(targetId, delta);
        }
    }

    private int getTargetVoteCount(String targetId, Vote.VoteTargetType targetType) {
        if (targetType == Vote.VoteTargetType.POST) {
            return postService.findById(targetId).map(Post::getVoteCount).orElse(0);
        } else {
            return commentService.findById(targetId).map(Comment::getVoteCount).orElse(0);
        }
    }
}
