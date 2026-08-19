package uz.tune.mentourBiz.rest.domain.schoolManagement.schedule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.enums.GroupScheduleStatus;

import java.time.Instant;
import java.util.UUID;

@Table(name = "group_schedules")
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupSchedule extends BaseEntity {

    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "due_date")
    private Instant dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(name = "schedule_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private GroupScheduleStatus status = GroupScheduleStatus.ACTIVE ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = true)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private CourseLesson lesson;

    @Column(name = "is_notification_sent")
    private Boolean isNotificationSent = false;

    @Column(name = "notification_sent_at")
    private java.time.Instant notificationSentAt = Instant.now();

}
