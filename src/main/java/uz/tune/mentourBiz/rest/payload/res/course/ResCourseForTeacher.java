package uz.tune.mentourBiz.rest.payload.res.course;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ResCourseForTeacher {
    private UUID courseId;
    private String courseName;
    private UUID groupUuid;

    public ResCourseForTeacher(Course course) {
        this.courseId = course.getUuid();
        this.courseName = course.getName();
        this.groupUuid = course.getGroup().getUuid();
    }
}
