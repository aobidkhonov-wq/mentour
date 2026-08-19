package uz.tune.mentourBiz.rest.payload.req.enrollment;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Bulk-assigns a {@link uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.BillingPlan}
 * (package) to a set of students inside one group. Drives the "assign package" form: students +
 * plan + billing start date + what to do when a student already has an active package.
 */
@Getter
@Setter
public class ReqAssignBillingPlan {

    private List<UUID> studentUuids;

    private UUID billingPlanId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    private Boolean replaceExistingPlan = Boolean.FALSE;
}
