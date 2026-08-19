package uz.tune.mentourBiz.rest.payload.res.course;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class ResCreateCourse {
    private String message;
    private UUID id;
    private Integer lastLessonIndex;
}
