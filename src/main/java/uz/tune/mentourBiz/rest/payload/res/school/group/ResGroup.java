package uz.tune.mentourBiz.rest.payload.res.school.group;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.payload.res.school.ResBranch;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ResGroup {
    private UUID id;
    private String name;
    private String branchName;
    private String teacherFullName;
    private String packageName;
    private String nextPaymentDate;
    private UUID teacherId;
    private ResBranch branchId;
    private ResSchoolInfo schoolInfo;
    private ResLevelInfoForGroup level;
    private Integer studentCount;
    private ResAttachment teacherAttachment;
    private Double healthScore;
    private Double attendanceScore;
    private Double academicScore;

    // Card metrics
    private Double attendancePercentage;
    private Double averageScorePercentage;
    private Long totalLessons;
    private Long paymentsDueCount;
    private Long paymentsDueAmount;

    private ResNextLesson nextLesson;
    private List<String> lessonDays;

    public ResGroup(Group group) {
        this.id = group.getUuid();
        this.name = group.getName();
        if (group.getBranch() != null) {
            this.branchName = group.getBranch().getSchool().getName();
            this.branchId = new ResBranch(group.getBranch());
            this.schoolInfo = new ResSchoolInfo(group.getBranch().getSchool());
        }
        this.level = new ResLevelInfoForGroup(group);
        this.lessonDays = group.getLessonDays();
        if (group.getTeacher() != null) {
            this.teacherFullName = group.getTeacher().getUser().getFirstName() + " " + group.getTeacher().getUser().getLastName();
            this.teacherId = group.getTeacher().getUser().getUuid();
        }
    }

    public ResGroup(Group group, long studentCount) {
        this(group);
        this.studentCount = (int) studentCount;
        if (group.getTeacher() != null && group.getTeacher().getUser().getAttachment() != null) {
            this.teacherAttachment = new ResAttachment(group.getTeacher().getUser().getAttachment());
        }
    }

    public ResGroup(Enrollment enrollment) {
        this(enrollment.getGroup());
        if (enrollment.getBillingPlan() != null) {
            this.packageName=enrollment.getBillingPlan().getName();
        }
        if (enrollment.getPaidUntil() != null) {
            this.nextPaymentDate = enrollment.getPaidUntil().toString();
        }
    }
}