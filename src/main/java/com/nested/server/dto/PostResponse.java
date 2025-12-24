package com.nested.server.dto;

import com.nested.server.model.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private String id;
    private String title;
    private String content;
    private String url;
    private String thumbnailUrl;
    private List<String> imageUrls;
    private Post.PostType postType;
    private String authorId;
    private String authorUsername;
    private String subId;
    private String subName;
    private String flair;
    private String flairColor;
    private int voteCount;
    private int commentCount;
    private String createdAt;
    private String timeAgo;
    private boolean nsfw;
    private boolean spoiler;
    private boolean locked;
    private boolean pinned;
    private boolean removed;
    private String removalReason;
    private Integer userVote; // 1, -1, or null
    private boolean saved;
    private boolean hidden;
}
