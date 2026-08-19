package uz.tune.mentourBiz.rest.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.enums.NotificationTargetType;

import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification extends BaseEntity {

    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private NotificationTargetType targetType; //    ALL, SCHOOL, GROUP, INDIVIDUAL

    @Column(name = "target_uuid")
    private UUID targetUuid;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "localizations", columnDefinition = "TEXT")
    private String localizations; // JSON: {"UZB":{"title":"...","content":"..."},"RUS":{...},...}

    // For in app display later
    @Column(name = "is_read")
    private boolean isRead = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id")
    private Attachment attachment;
}