package com.nested.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private String id;
    private String content;
    private String authorId;
    private String authorUsername;
    private String postId;
    private String parentCommentId;
    private int voteCount;
    private int depth;
    private String createdAt;
    private String timeAgo;
    private boolean deleted;
    private Integer userVote;
    @Builder.Default
    private List<CommentResponse> replies = new ArrayList<>();
}
