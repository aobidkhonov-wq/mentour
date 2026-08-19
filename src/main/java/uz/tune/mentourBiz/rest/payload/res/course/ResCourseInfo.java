package uz.tune.mentourBiz.rest.payload.res.course;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;

@Getter
@Setter
public class ResCourseInfo {

    private String courseName;
    private String schoolName;
    private String description;
    private Long numberOfLessons;
    private String mentorName;
    private String moderatorName;


    public ResCourseInfo(Course course, Long numberOfLessons) {
        this.courseName = course.getName();
        this.schoolName = course.getSchool().getName();
        this.description = course.getDescription();
        this.numberOfLessons = numberOfLessons;
        this.mentorName = course.getMentor().getUser().getFirstName() + " " + course.getMentor().getUser().getLastName();
        this.moderatorName = course.getModerator().getUser().getFirstName() + " " + course.getModerator().getUser().getLastName();
    }
}