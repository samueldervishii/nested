package com.nested.dto;

import com.nested.model.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 300, message = "Title must be less than 300 characters")
    private String title;

    private String content;

    private String url;

    @NotBlank(message = "Community is required")
    private String subnestedName;

    private Post.PostType postType = Post.PostType.TEXT;

    private String flair;

    private boolean nsfw = false;

    private boolean spoiler = false;
}
