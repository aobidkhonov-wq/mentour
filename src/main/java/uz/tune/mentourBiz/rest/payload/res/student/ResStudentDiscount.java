package uz.tune.mentourBiz.rest.payload.res.student;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.userManagement.user.StudentDiscount;
import uz.tune.mentourBiz.rest.enums.DiscountType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class ResStudentDiscount {

    private UUID uuid;
    private UUID studentUuid;
    private String studentName;
    private DiscountType type;
    private Long amount;
    private Integer percent;
    private LocalDate startDate;
    private Integer durationMonths;
    private LocalDate endDate;
    private Boolean permanent;
    private Boolean isActive;
    // Whether the discount is actually in force today (switched on and inside its date window).
    private Boolean inForce;
    private String note;
    private Instant createdAt;

    public ResStudentDiscount(StudentDiscount discount, LocalDate today) {
        this.uuid = discount.getUuid();
        this.type = discount.getType();
        this.amount = discount.getAmount();
        this.percent = discount.getPercent();
        this.startDate = discount.getStartDate();
        this.durationMonths = discount.getDurationMonths();
        this.endDate = discount.getEndDate();
        this.permanent = discount.isPermanent();
        this.isActive = discount.getIsActive();
        this.inForce = discount.appliesOn(today);
        this.note = discount.getNote();
        this.createdAt = discount.getCreatedAt();

        if (discount.getStudent() != null) {
            this.studentUuid = discount.getStudent().getUuid();
            if (discount.getStudent().getUser() != null) {
                this.studentName = discount.getStudent().getUser().getFirstName()
                        + " " + discount.getStudent().getUser().getLastName();
            }
        }
    }
}
