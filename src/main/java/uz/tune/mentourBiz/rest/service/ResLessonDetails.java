package uz.tune.mentourBiz.rest.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;

import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ResLessonDetails {
    private UUID uuid;
    private String name;

    public ResLessonDetails(CourseLesson courseLesson) {
        this.uuid = courseLesson.getUuid();
        this.name = courseLesson.getName();
    }
}
