package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResTeacherGroup {

    private Long groupId;
    private UUID groupUuid;
    private String groupName;
    private String groupStatus;
    private Long totalStudents;
    private Long activeCourses;
    private Long finishedCourses;
    private Long totalCourses;
    private Long totalLessons;
    private Long markedLessons;
    private Double markingRate;
    private Double attendanceRate;
    private List<ResTeacherCourse> courses;

}
