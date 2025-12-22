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
@Document(collection = "votes")
@CompoundIndexes({
    @CompoundIndex(name = "user_target_idx", def = "{'userId': 1, 'targetId': 1, 'targetType': 1}", unique = true),
    @CompoundIndex(name = "target_type_idx", def = "{'targetId': 1, 'targetType': 1}")
})
public class Vote {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String targetId;

    private VoteTargetType targetType;

    private VoteType voteType;

    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum VoteTargetType {
        POST,
        COMMENT
    }

    public enum VoteType {
        UPVOTE(1),
        DOWNVOTE(-1);

        private final int value;

        VoteType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
