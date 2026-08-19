package uz.tune.mentourBiz.rest.payload.req.enrollment;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.BillingPlanType;

import java.util.UUID;

@Getter
@Setter
public class ReqBillingPlan {
    private String name;
    private BillingPlanType type;
    private Long price;
    private Integer lessonCount;
    private Integer countMonth;
    private Boolean isActive;
    private UUID schoolUuid;
}
