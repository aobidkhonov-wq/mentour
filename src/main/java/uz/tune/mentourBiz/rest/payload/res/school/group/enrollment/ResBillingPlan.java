package uz.tune.mentourBiz.rest.payload.res.school.group.enrollment;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.BillingPlan;
import uz.tune.mentourBiz.rest.enums.BillingPlanType;

import java.util.UUID;

@Getter
@Setter
public class ResBillingPlan {
    private UUID uuid;
    private String name;
    private BillingPlanType type;
    private Long price;
    private Integer lessonCount;
    private Integer countMonth;
    private Boolean isActive;
    private UUID schoolUuid;
    private String schoolName;
    private String regionName;

    public ResBillingPlan(BillingPlan plan) {
        this.uuid = plan.getUuid();
        this.name = plan.getName();
        this.type = plan.getType();
        this.price = plan.getPrice();
        this.lessonCount = plan.getLessonCount();
        this.countMonth = plan.getCountMonth();
        this.isActive = plan.getIsActive();
        if (plan.getSchool() != null) {
            this.schoolUuid = plan.getSchool().getUuid();
            this.schoolName = plan.getSchool().getName();
            this.regionName = plan.getSchool().getRegion().getName();
        }
    }
}
