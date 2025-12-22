package com.nested.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "posts")
@CompoundIndexes({
    @CompoundIndex(name = "subnested_created", def = "{'subnestedId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "subnested_votes", def = "{'subnestedId': 1, 'voteCount': -1}"),
    @CompoundIndex(name = "author_created", def = "{'authorId': 1, 'createdAt': -1}")
})
public class Post {

    @Id
    private String id;

    private String title;

    private String content;

    private String url;

    private String thumbnailUrl;

    private PostType postType;

    @Indexed
    private String authorId;

    private String authorUsername;

    @Indexed
    private String subnestedId;

    private String subnestedName;

    private String flair;

    private String flairColor;

    @Builder.Default
    private int voteCount = 0;

    @Builder.Default
    private int commentCount = 0;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @Builder.Default
    private boolean nsfw = false;

    @Builder.Default
    private boolean spoiler = false;

    @Builder.Default
    private boolean locked = false;

    public enum PostType {
        TEXT,
        LINK,
        IMAGE,
        VIDEO
    }
}
