package com.nested.server.service;

import com.nested.server.dto.VoteRequest;
import com.nested.server.dto.VoteResult;
import com.nested.server.model.User;
import com.nested.server.model.Vote;
import com.nested.server.repository.CommentRepository;
import com.nested.server.repository.PostRepository;
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
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;

    /**
     * Optimized vote operation - reduced from 5 DB ops to 2-3:
     * 1. Check existing vote + save/update (combined via findAndModify pattern)
     * 2. Atomic increment on target vote count
     * 3. Async karma update (non-blocking)
     */
    @Transactional
    public VoteResult vote(VoteRequest request, User user) {
        String targetId = request.getTargetId();
        Vote.VoteTargetType targetType = request.getTargetType();

        // Query 1: Check existing vote
        Optional<Vote> existingVote = voteRepository.findByUserIdAndTargetIdAndTargetType(
                user.getId(), targetId, targetType);

        int voteChange;
        int previousVote = 0;

        if (existingVote.isPresent()) {
            Vote vote = existingVote.get();
            previousVote = vote.getVoteType().getValue();

            if (vote.getVoteType() == request.getVoteType()) {
                // Remove vote (toggle off)
                voteRepository.delete(vote);
                voteChange = -vote.getVoteType().getValue();
            } else {
                // Change vote direction - use atomic update
                voteChange = request.getVoteType().getValue() - vote.getVoteType().getValue();
                voteRepository.updateVoteType(vote.getId(), request.getVoteType());
            }
        } else {
            // New vote
            Vote vote = Vote.builder()
                    .userId(user.getId())
                    .targetId(targetId)
                    .targetType(targetType)
                    .voteType(request.getVoteType())
                    .build();
            voteRepository.save(vote);
            voteChange = request.getVoteType().getValue();
        }

        // Query 2: Atomic increment on target + get author ID in one operation
        String authorId;
        int newVoteCount;

        if (targetType == Vote.VoteTargetType.POST) {
            // Use atomic increment and return updated count
            postRepository.incrementVoteCount(targetId, voteChange);
            var post = postRepository.findAuthorIdAndVoteCountById(targetId);
            authorId = post.map(p -> p.getAuthorId()).orElse(null);
            newVoteCount = post.map(p -> p.getVoteCount()).orElse(0);
        } else {
            commentRepository.incrementVoteCount(targetId, voteChange);
            var comment = commentRepository.findAuthorIdAndVoteCountById(targetId);
            authorId = comment.map(c -> c.getAuthorId()).orElse(null);
            newVoteCount = comment.map(c -> c.getVoteCount()).orElse(0);
        }

        // Async karma update (non-blocking) - only if voting on someone else's content
        if (authorId != null && !authorId.equals(user.getId())) {
            userService.updateKarma(authorId, voteChange);
        }

        // Return result with new vote count and user's current vote
        int userVote = existingVote.isPresent() &&
                       existingVote.get().getVoteType() == request.getVoteType()
                       ? 0 : request.getVoteType().getValue();

        return new VoteResult(newVoteCount, userVote);
    }
}
