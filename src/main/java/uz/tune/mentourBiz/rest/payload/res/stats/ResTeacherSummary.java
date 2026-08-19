package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResTeacherSummary {

    private Long totalTeachers;
    private Long teachersWithGroups;
    private Long teachersWithoutGroups;
    private Long activeCourses;
    private Double markingRateLast30Days;

}
