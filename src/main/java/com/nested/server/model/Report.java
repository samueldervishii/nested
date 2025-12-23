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
@Document(collection = "reports")
@CompoundIndexes({
        @CompoundIndex(name = "sub_status", def = "{'subId': 1, 'status': 1}"),
        @CompoundIndex(name = "target", def = "{'targetId': 1, 'targetType': 1}")
})
public class Report {

    @Id
    private String id;

    @Indexed
    private String reporterId;

    private String reporterUsername;

    private TargetType targetType;

    @Indexed
    private String targetId;

    private ReportReason reason;

    private String description;

    @Indexed
    private String subId;

    private String subName;

    @Indexed
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    private String reviewedBy;

    private String reviewedByUsername;

    private Instant reviewedAt;

    private String modNote;

    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum TargetType {
        POST,
        COMMENT,
        USER
    }

    public enum ReportReason {
        SPAM,
        HARASSMENT,
        HATE_SPEECH,
        MISINFORMATION,
        NSFW_CONTENT,
        BREAKS_RULES,
        OTHER
    }

    public enum ReportStatus {
        PENDING,
        REVIEWED,
        RESOLVED,
        DISMISSED
    }
}
