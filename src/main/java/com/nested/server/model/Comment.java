package com.nested.server.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "comments")
@CompoundIndexes({
        @CompoundIndex(name = "post_created", def = "{'postId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "post_votes", def = "{'postId': 1, 'voteCount': -1}")
})
public class Comment {

    @Id
    private String id;

    private String content;

    @Indexed
    private String authorId;

    private String authorName;

    @Indexed
    private String postId;

    @Indexed
    private String parentCommentId;

    @Builder.Default
    private int voteCount = 0;

    @Builder.Default
    private int depth = 0;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @Builder.Default
    private boolean deleted = false;

    @Builder.Default
    private boolean removed = false;

    private String removedById;

    private String removedByUsername;

    private String removalReason;
}
