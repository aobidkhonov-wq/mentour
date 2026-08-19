package uz.tune.mentourBiz.rest.domain.schoolManagement.course;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.PaymentPackage;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.SchoolBook;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Mentor;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Moderator;
import uz.tune.mentourBiz.rest.enums.CourseStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table(name = "courses")
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Course extends BaseEntity {

    @Column(name = "uuid")
    private UUID uuid = UUID.randomUUID();

    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id")
    private Mentor mentor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id")
    private Moderator moderator;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "duration")
    private Integer duration;

    // Chosen at creation (defaults to "now"); the course is "overdue" once
    // courseStartDate + school's course length has passed. Soft indicator only.
    @Column(name = "course_start_date")
    private java.time.Instant courseStartDate;

    @Formula("(SELECT count(*) FROM course_lessons cl WHERE cl.course_id = id AND cl.status != 'DELETED')")
    private Long lessonCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CourseStatus status = CourseStatus.ACTIVE;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("statusOrder ASC, startTime ASC")
    private List<CourseLesson> lessons;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "book_id")
//    private SchoolBook books;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_package_id")
    private PaymentPackage paymentPackage;


    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "course_books",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "school_book_id")
    )
    private List<SchoolBook> linkedBooks = new ArrayList<>();

}