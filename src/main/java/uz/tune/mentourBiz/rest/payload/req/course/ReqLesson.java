package uz.tune.mentourBiz.rest.payload.req.course;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.LessonType;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReqLesson {
    private String name;
    private String date; //yyyy-MM-dd
    private String startTime; // HH:mm
    private String endTime; // HH:mm
    private String link;
    private List<UUID> unitUuids;
    private LessonType lessonType = LessonType.APP;
    private String description;
}