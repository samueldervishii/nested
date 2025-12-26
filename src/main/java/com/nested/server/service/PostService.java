package com.nested.server.service;

import com.nested.server.dto.PostRequest;
import com.nested.server.dto.PostResponse;
import com.nested.server.exception.ResourceNotFoundException;
import com.nested.server.exception.UnauthorizedException;
import com.nested.server.model.Post;
import com.nested.server.model.Subs;
import com.nested.server.model.User;
import com.nested.server.model.Vote;
import com.nested.server.repository.PostRepository;
import com.nested.server.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final SubService subService;
    private final VoteRepository voteRepository;

    public PostResponse createPost(PostRequest request, User author) {
        Subs subs = subService.findByName(request.getSubName())
                .orElseThrow(() -> new ResourceNotFoundException("Sub", "name", request.getSubName()));

        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .url(request.getUrl())
                .imageUrls(request.getImageUrls() != null ? request.getImageUrls() : new java.util.ArrayList<>())
                .postType(request.getPostType())
                .authorId(author.getId())
                .authorUsername(author.getUsername())
                .subId(subs.getId())
                .subName(subs.getName())
                .flair(request.getFlair())
                .nsfw(request.isNsfw())
                .spoiler(request.isSpoiler())
                .voteCount(1)
                .createdAt(Instant.now())
                .build();

        post = postRepository.save(post);

        // Auto-upvote own post
        Vote vote = Vote.builder()
                .userId(author.getId())
                .targetId(post.getId())
                .targetType(Vote.VoteTargetType.POST)
                .voteType(Vote.VoteType.UPVOTE)
                .build();
        voteRepository.save(vote);

        return mapToResponse(post, 1);
    }

    public Optional<Post> findById(String id) {
        return postRepository.findById(id);
    }

    public List<PostResponse> getHomeFeed(User user, String sort, int page, int size) {
        Pageable pageable = createPageable(sort, page, size);
        Page<Post> posts;

        if (user != null && !user.getSubscribedSubs().isEmpty()) {
            posts = postRepository.findBySubIdIn(user.getSubscribedSubs(), pageable);
            // Fall back to all posts if subscribed feed is empty
            if (posts.isEmpty()) {
                posts = postRepository.findAll(pageable);
            }
        } else {
            posts = postRepository.findAll(pageable);
        }

        return mapPostsWithUserVotes(posts.getContent(), user);
    }

    public List<PostResponse> getPostsBySubs(String subName, String sort, int page, int size, User user) {
        Pageable pageable = createPageable(sort, page, size);
        Page<Post> posts = postRepository.findBySubName(subName, pageable);
        List<Post> postList = posts.getContent();

        // Sort pinned posts first (only on first page)
        if (page == 0) {
            postList = postList.stream()
                    .sorted((p1, p2) -> {
                        if (p1.isPinned() && !p2.isPinned()) return -1;
                        if (!p1.isPinned() && p2.isPinned()) return 1;
                        return 0;
                    })
                    .collect(Collectors.toList());
        }

        return mapPostsWithUserVotes(postList, user);
    }

    public List<PostResponse> getPostsByUser(String authorId, int page, int size, User currentUser) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts = postRepository.findByAuthorId(authorId, pageable);
        return mapPostsWithUserVotes(posts.getContent(), currentUser);
    }

    public List<PostResponse> getPopularPosts(User user) {
        List<Post> posts = postRepository.findTop100ByOrderByVoteCountDesc();
        return mapPostsWithUserVotes(posts, user);
    }

    public List<PostResponse> getNewPosts(User user) {
        List<Post> posts = postRepository.findTop100ByOrderByCreatedAtDesc();
        return mapPostsWithUserVotes(posts, user);
    }

    public List<PostResponse> getHotPosts(User user) {
        // Hot posts from last 7 days, sorted by votes (fallback to all posts if empty)
        Instant since = Instant.now().minus(Duration.ofDays(7));
        List<Post> posts = postRepository.findTop100ByCreatedAtAfterOrderByVoteCountDesc(since);
        if (posts.isEmpty()) {
            posts = postRepository.findTop100ByOrderByVoteCountDesc();
        }
        return mapPostsWithUserVotes(posts, user);
    }

    /**
     * Atomic comment count update using MongoDB $inc operator
     */
    public void updateCommentCount(String postId, int delta) {
        postRepository.incrementCommentCount(postId, delta);
    }

    /**
     * Atomic vote count update using MongoDB $inc operator
     */
    public void updateVoteCount(String postId, int delta) {
        postRepository.incrementVoteCount(postId, delta);
    }

    private Pageable createPageable(String sort, int page, int size) {
        Sort sortOrder = switch (sort.toLowerCase()) {
            case "new" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "top" -> Sort.by(Sort.Direction.DESC, "voteCount");
            case "controversial" -> Sort.by(Sort.Direction.ASC, "voteCount");
            default -> Sort.by(Sort.Direction.DESC, "voteCount", "createdAt");
        };
        return PageRequest.of(page, size, sortOrder);
    }

    private List<PostResponse> mapPostsWithUserVotes(List<Post> posts, User user) {
        if (user == null) {
            return posts.stream()
                    .map(post -> mapToResponse(post, null))
                    .collect(Collectors.toList());
        }

        List<String> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        List<Vote> userVotes = voteRepository.findByUserIdAndTargetIdIn(user.getId(), postIds);
        Map<String, Integer> voteMap = userVotes.stream()
                .collect(Collectors.toMap(Vote::getTargetId, v -> v.getVoteType().getValue()));

        return posts.stream()
                .map(post -> mapToResponse(post, voteMap.get(post.getId())))
                .collect(Collectors.toList());
    }

    public PostResponse mapToResponse(Post post, Integer userVote) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .url(post.getUrl())
                .thumbnailUrl(post.getThumbnailUrl())
                .imageUrls(post.getImageUrls())
                .postType(post.getPostType())
                .authorId(post.getAuthorId())
                .authorUsername(post.getAuthorUsername())
                .subId(post.getSubId())
                .subName(post.getSubName())
                .flair(post.getFlair())
                .flairColor(post.getFlairColor())
                .voteCount(post.getVoteCount())
                .commentCount(post.getCommentCount())
                .createdAt(formatDate(post.getCreatedAt()))
                .timeAgo(getTimeAgo(post.getCreatedAt()))
                .nsfw(post.isNsfw())
                .spoiler(post.isSpoiler())
                .locked(post.isLocked())
                .pinned(post.isPinned())
                .removed(post.isRemoved())
                .removalReason(post.getRemovalReason())
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
        } else if (duration.toDays() < 365) {
            long months = duration.toDays() / 30;
            return months + (months == 1 ? " month ago" : " months ago");
        } else {
            long years = duration.toDays() / 365;
            return years + (years == 1 ? " year ago" : " years ago");
        }
    }

    public PostResponse updatePost(String postId, Map<String, String> updates, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (!post.getAuthorId().equals(user.getId())) {
            throw new UnauthorizedException("You can only edit your own posts");
        }

        if (updates.containsKey("content")) {
            post.setContent(updates.get("content"));
        }
        if (updates.containsKey("flair")) {
            post.setFlair(updates.get("flair"));
        }
        if (updates.containsKey("nsfw")) {
            post.setNsfw(Boolean.parseBoolean(updates.get("nsfw")));
        }
        if (updates.containsKey("spoiler")) {
            post.setSpoiler(Boolean.parseBoolean(updates.get("spoiler")));
        }

        post = postRepository.save(post);
        return mapToResponse(post, null);
    }

    public void deletePost(String postId, User user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (!post.getAuthorId().equals(user.getId())) {
            throw new UnauthorizedException("You can only delete your own posts");
        }

        postRepository.delete(post);
    }

    public List<PostResponse> searchPosts(String query, int page, int size, User user) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts;

        // Use text search if query has actual content
        if (query != null && query.trim().length() >= 2) {
            try {
                posts = postRepository.searchByText(query.trim(), pageable);
            } catch (Exception e) {
                log.debug("Text search failed, falling back to regex: {}", e.getMessage());
                // Fallback to regex search if text index not available
                posts = postRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
                        query, query, pageable);
            }
        } else {
            posts = Page.empty();
        }

        return mapPostsWithUserVotes(posts.getContent(), user);
    }

    public List<PostResponse> getSavedPosts(User user, int page, int size) {
        Set<String> savedPostIds = user.getSavedPosts();
        if (savedPostIds == null || savedPostIds.isEmpty()) {
            return List.of();
        }

        List<Post> posts = postRepository.findByIdIn(savedPostIds);
        return mapPostsWithUserVotes(posts, user);
    }

    public PostResponse mapToResponseWithUserData(Post post, User user) {
        Integer userVote = null;
        boolean saved = false;
        boolean hidden = false;

        if (user != null) {
            List<Vote> votes = voteRepository.findByUserIdAndTargetIdIn(user.getId(), List.of(post.getId()));
            if (!votes.isEmpty()) {
                userVote = votes.get(0).getVoteType().getValue();
            }
            saved = user.getSavedPosts() != null && user.getSavedPosts().contains(post.getId());
            hidden = user.getHiddenPosts() != null && user.getHiddenPosts().contains(post.getId());
        }

        PostResponse response = mapToResponse(post, userVote);
        response.setSaved(saved);
        response.setHidden(hidden);
        return response;
    }
}
