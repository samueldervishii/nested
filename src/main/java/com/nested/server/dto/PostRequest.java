package com.nested.server.dto;

import com.nested.server.model.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 300, message = "Title must be between 1 and 300 characters")
    private String title;

    @Size(max = 40000, message = "Content must be less than 40000 characters")
    private String content;

    @Size(max = 2048, message = "URL is too long")
    @Pattern(regexp = "^(https?://.*)?$", message = "Invalid URL format")
    private String url;

    @NotBlank(message = "Community is required")
    @Size(max = 50, message = "Community name is too long")
    private String subName;

    private Post.PostType postType = Post.PostType.TEXT;

    @Size(max = 64, message = "Flair is too long")
    private String flair;

    private boolean nsfw = false;

    private boolean spoiler = false;

    private List<String> imageUrls = new ArrayList<>();
}
