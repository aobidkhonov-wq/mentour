package uz.tune.mentourBiz.rest.payload.req.enrollment;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReqEnrollmentCreate {
    private UUID groupId;
    private List<UUID> studentIds;
    // Coin billing plan the students are charged for to join the group (12 lessons / monthly, etc.).
    private UUID billingPlanId;
}