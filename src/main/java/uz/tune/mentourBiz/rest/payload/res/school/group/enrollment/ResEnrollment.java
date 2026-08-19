package uz.tune.mentourBiz.rest.payload.res.school.group.enrollment;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;

import java.util.UUID;

@Getter
@Setter
public class ResEnrollment {
    private UUID uuid;
    private UUID studentId;
    private String studentFullName;
    private UUID groupId;
    private String groupName;
    private EnrollmentStatus status;

    public ResEnrollment(Enrollment enrollment) {
        this.uuid = enrollment.getUuid();
        this.status = enrollment.getStatus();
        if (enrollment.getStudent() != null) {
            this.studentId = enrollment.getStudent().getUser().getUuid();
            this.studentFullName = enrollment.getStudent().getUser().getFirstName() + " " + enrollment.getStudent().getUser().getLastName();
        }
        if (enrollment.getGroup() != null) {
            this.groupId = enrollment.getGroup().getUuid();
            this.groupName = enrollment.getGroup().getName();
        }
    }
}