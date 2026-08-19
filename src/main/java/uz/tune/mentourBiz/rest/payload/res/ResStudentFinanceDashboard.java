package uz.tune.mentourBiz.rest.payload.res;

import lombok.Data;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.StudentDiscount;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.payload.res.student.ResStudentDiscount;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class ResStudentFinanceDashboard {
    private UUID id;
    private String studentName;
    private String teacherFullName;
    private List<GroupInfo> groups;
    private Long balance;
    private FinanceEnums.FinanceStatus status;
    private UserStatus userStatus;
    private ResAttachment attachment;
    private String userName;
    // The student's discounts in force today — one per type, so 0, 1 or 2. Empty for everybody else.
    private List<ResStudentDiscount> discounts;

    @Data
    public static class GroupInfo {
        private UUID uuid;
        private String name;
        // The billing plan / package the student is on *for this specific group*. Null when the
        // student's ongoing enrollment in this group has no billing plan attached.
        private ResPaymentPackage resPaymentPackage;
        // Enrollment.paidUntil: the instant the current paid period runs out (next payment is due).
        // Null for LESSON_PACK or when no billing plan / period is set.
        private Instant nextPaymentDate;
        // What the student's discounts together take off this group's plan price, and what is left to
        // pay. Both null when there is no discount or no billing plan.
        private Long discountAmount;
        private Long payablePrice;

        /** Prices this group's plan for a discounted student. No-op when either side is missing. */
        public void applyDiscounts(List<StudentDiscount> discounts) {
            if (discounts == null || discounts.isEmpty()
                    || resPaymentPackage == null || resPaymentPackage.getPrice() == null) return;
            long price = resPaymentPackage.getPrice();
            this.discountAmount = StudentDiscount.totalDiscountOn(discounts, price);
            this.payablePrice = price - this.discountAmount;
        }

        public GroupInfo(Group group) {
            this.uuid = group.getUuid();
            this.name = group.getName();
        }

        public GroupInfo(Enrollment enrollment) {
            Group group = enrollment.getGroup();
            this.uuid = group.getUuid();
            this.name = group.getName();
            this.resPaymentPackage = enrollment.getBillingPlan() != null
                    ? new ResPaymentPackage(enrollment.getBillingPlan())
                    : null;
            this.nextPaymentDate = enrollment.getPaidUntil();
        }
    }
}
