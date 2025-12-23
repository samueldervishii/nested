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
@Document(collection = "user_flairs")
@CompoundIndexes({
        @CompoundIndex(name = "sub_user", def = "{'subId': 1, 'userId': 1}", unique = true)
})
public class UserFlair {

    @Id
    private String id;

    @Indexed
    private String subId;

    @Indexed
    private String userId;

    private String text;

    @Builder.Default
    private String textColor = "#000000";

    @Builder.Default
    private String backgroundColor = "#EDEFF1";

    private String assignedById;

    private String assignedByUsername;

    @Builder.Default
    private boolean userEditable = true;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant updatedAt;
}
