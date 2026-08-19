package uz.tune.mentourBiz.rest.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;

import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ResCourseDetails {
    private UUID uuid;
    private String name;

    public ResCourseDetails(Course course) {
        this.uuid = course.getUuid();
        this.name = course.getName();
    }
}
