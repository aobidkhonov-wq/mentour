package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * A teacher's stored salary configuration, as saved. Returned by both the setup call and the read call
 * so the admin UI can bind the form directly to it.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResTeacherSalaryPlan {

    private UUID teacherUuid;
    private String teacherName;

    // False when the teacher has no plan row yet; the amounts below are then all zero.
    private Boolean configured;

    private Long fixedSalary;
    private Integer percentPerGroup;
    private Long fixedPerStudent;
    private Boolean isActive;

    // Groups paid at a percent other than percentPerGroup. Empty when there are none.
    private List<GroupOverride> groupOverrides;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GroupOverride {
        private UUID groupUuid;
        private String groupName;
        private Integer percent;
    }
}
