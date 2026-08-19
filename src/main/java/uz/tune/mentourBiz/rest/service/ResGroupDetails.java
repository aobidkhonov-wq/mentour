package uz.tune.mentourBiz.rest.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;

import java.util.List;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResGroupDetails {
    private UUID uuid;
    private String name;
    private List<ResLessonDetails> lessonDetails;
    private List<ResCourseDetails> courseDetails;

    public ResGroupDetails(Group group, List<ResLessonDetails> lessonDetails) {
        this.uuid = group.getUuid();
        this.name = group.getName();
        this.lessonDetails = lessonDetails;
    }

    public static ResGroupDetails forCourses(Group group, List<ResCourseDetails> courseDetails) {
        ResGroupDetails details = new ResGroupDetails(group, null);
        details.setCourseDetails(courseDetails);
        return details;
    }
}
