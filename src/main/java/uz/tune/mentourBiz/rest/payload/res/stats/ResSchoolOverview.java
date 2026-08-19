package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResSchoolOverview {

    private Long totalStudents;
    private Long totalTeachers;
    private Long totalLessonsHeld;
    private Double attendanceRate;
    private Long activeStudentsThisMonth;
    private Long activeTeachersThisMonth;
    private Long totalEnrolled;
    private Long totalCollected;

}
