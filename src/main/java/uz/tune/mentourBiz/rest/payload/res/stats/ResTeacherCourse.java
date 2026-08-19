package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResTeacherCourse {

    private Long courseId;
    private String courseName;
    private Long groupId;
    private String groupName;
    private Long totalStudents;
    private Long totalLessons;
    private Long markedLessons;
    private Double markingRate;
    private Double studentAttendanceRate;
    private String courseStatus;
    private Instant courseCreatedAt;
    private Instant courseFinishedAt;

}
