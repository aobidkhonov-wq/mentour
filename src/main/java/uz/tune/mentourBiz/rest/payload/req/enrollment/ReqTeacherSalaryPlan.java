package uz.tune.mentourBiz.rest.payload.req.enrollment;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Setup payload for a teacher's salary plan. Replaces the plan wholesale — every field is optional and
 * an omitted one is stored as its zero value, so a request carrying only {@code teacherUuid} resets the
 * teacher to "paid nothing". Send the full picture each time.
 *
 * <p>{@code groupOverrides} lists only the groups whose percent differs from {@code percentPerGroup};
 * omitting it (or sending an empty list) clears every existing override.
 */
@Getter
@Setter
public class ReqTeacherSalaryPlan {

    // Required: the teacher's user uuid.
    private UUID teacherUuid;

    // Flat monthly base. Null -> 0.
    private Long fixedSalary;

    // Percent (0..100) of each group's collected revenue. Null -> 0.
    @JsonAlias("revenueSharePercent")
    private Integer percentPerGroup;

    // Flat amount per student per month. Null -> 0.
    private Long fixedPerStudent;

    // Null -> true.
    private Boolean isActive;

    // Null or empty -> all existing overrides are removed.
    private List<GroupOverride> groupOverrides;

    @Getter
    @Setter
    public static class GroupOverride {
        private UUID groupUuid;

        // Percent (0..100) for this group instead of percentPerGroup. Required within an override entry.
        @JsonAlias("overridePercent")
        private Integer percent;
    }
}
