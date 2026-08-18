package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Salary breakdown for one group of a teacher, for a given month. The teacher's cut of a group is
 * the group's billed revenue scaled by the teacher's lesson share (how many of the group's lessons
 * that month they conducted), times the revenue-share percent.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResTeacherGroupPayroll {
    private UUID groupUuid;
    private String groupName;

    private Long studentCount;

    private Long billedStudentCount;

    private Boolean substitute;

    private Long groupBilledRevenue;
    private Long groupCollectedRevenue;

    private Long totalLessons;
    private Long teacherLessons;
    private Double lessonShare;

    private Long teacherBilledRevenue;

    private Long teacherCollectedRevenue;

    private Integer percent;


    private Long revenueShareSalary;

    private Long attendanceUnits;

    private Long perStudentSalary;

    private Long groupSalary;
}
