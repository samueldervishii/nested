package com.nested.server.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "posts")
@CompoundIndexes({
        @CompoundIndex(name = "sub_created", def = "{'subId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "sub_votes", def = "{'subId': 1, 'voteCount': -1}"),
        @CompoundIndex(name = "author_created", def = "{'authorId': 1, 'createdAt': -1}")
})
public class Post {

    @Id
    private String id;

    @TextIndexed(weight = 3)  // Title has higher weight in search
    private String title;

    @TextIndexed(weight = 1)
    private String content;

    private String url;

    private String thumbnailUrl;

    private PostType postType;

    @Indexed
    private String authorId;

    private String authorUsername;

    @Indexed
    private String subId;

    @Indexed
    private String subName;

    private String flair;

    private String flairColor;

    @Indexed
    @Builder.Default
    private int voteCount = 0;

    @Builder.Default
    private int commentCount = 0;

    @Indexed
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
