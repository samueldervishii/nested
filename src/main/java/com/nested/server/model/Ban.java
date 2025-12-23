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
@Document(collection = "bans")
@CompoundIndexes({
        @CompoundIndex(name = "sub_user", def = "{'subId': 1, 'userId': 1}", unique = true)
})
public class Ban {

    @Id
    private String id;

    @Indexed
    private String subId;

    private String subName;

    @Indexed
    private String userId;

    private String username;

    private String bannedById;

    private String bannedByUsername;

    private String reason;

    @Builder.Default
    private boolean permanent = true;

    private Instant expiresAt;

    @Builder.Default
    private Instant createdAt = Instant.now();

    public boolean isActive() {
        if (permanent) {
            return true;
        }
        return expiresAt != null && expiresAt.isAfter(Instant.now());
    }
}
