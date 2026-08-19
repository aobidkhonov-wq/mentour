package uz.tune.mentourBiz.rest.payload.res.school.group.enrollment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;

import java.util.UUID;

@Getter
@Setter
public class ResGroupStudent {

    private UUID id;
    private String studentName;
    private String groupName;
    private EnrollmentStatus status;
    private String teacher;

    // Student's currentBalance (som).
    private Long balance;

    private String packageName;

    @JsonProperty("package")
    private ResBillingPlan billingPlan;

    public ResGroupStudent(Enrollment enrollment) {
        this.id = enrollment.getUuid();
        this.status = enrollment.getStatus();

        if (enrollment.getStudent() != null) {
            this.balance = enrollment.getStudent().getCurrentBalance();
            if (enrollment.getStudent().getUser() != null) {
                this.studentName = enrollment.getStudent().getUser().getFirstName() + " "
                        + enrollment.getStudent().getUser().getLastName();
            }
        }

        if (enrollment.getGroup() != null) {
            this.groupName = enrollment.getGroup().getName();
            if (enrollment.getGroup().getTeacher() != null && enrollment.getGroup().getTeacher().getUser() != null) {
                this.teacher = enrollment.getGroup().getTeacher().getUser().getFirstName() + " "
                        + enrollment.getGroup().getTeacher().getUser().getLastName();
            }
        }

        if (enrollment.getBillingPlan() != null) {
            this.packageName = enrollment.getBillingPlan().getName();
            this.billingPlan = new ResBillingPlan(enrollment.getBillingPlan());
        }
    }
}
