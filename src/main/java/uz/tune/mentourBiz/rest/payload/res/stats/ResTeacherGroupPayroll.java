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

    // Students enrolled right now — a live figure, not the month's headcount.
    private Long studentCount;

    // Students this group actually billed during the month. This is the one to read on a past period.
    private Long billedStudentCount;

    // True when this teacher is NOT the group's regular teacher (they only substituted here).
    private Boolean substitute;

    // Group-wide figures for the period (all teachers combined).
    private Long groupBilledRevenue;      // net amount the group billed its students (accrual view)
    // Payments settled against this group's charges during the month (cash view). Can go negative in the
    // rare case a refund hands back money that was collected in an earlier month.
    private Long groupCollectedRevenue;

    // Lesson share: how many of the group's finished lessons this teacher conducted this month.
    private Long totalLessons;
    private Long teacherLessons;
    private Double lessonShare;           // teacherLessons / totalLessons

    // The teacher's slice of the group's billed revenue = groupBilledRevenue * lessonShare. This is what
    // the revenue share is actually paid on, so it is the figure the payslip should show next to percent.
    private Long teacherBilledRevenue;

    // The teacher's slice of the group's collected revenue = groupCollectedRevenue * lessonShare. Kept
    // for the cash view — how much of what this teacher earned has actually come in.
    private Long teacherCollectedRevenue;

    // Revenue share percent applied (per-group override or the plan default).
    private Integer percent;

    // --- Salary components for this group ---

    // Revenue-share part = teacherBilledRevenue * percent / 100.
    private Long revenueShareSalary;

    // Number of (student, lesson) attendance units the teacher conducted in this group this month.
    private Long attendanceUnits;

    // Fixed-per-student part = fixedPerStudent * attendanceUnits / totalLessons (prorated by lessons).
    private Long perStudentSalary;

    // revenueShareSalary + perStudentSalary.
    private Long groupSalary;
}
