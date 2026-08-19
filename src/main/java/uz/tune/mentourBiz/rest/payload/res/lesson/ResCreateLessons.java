package uz.tune.mentourBiz.rest.payload.res.lesson;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.LessonStatus;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResCreateLessons {
    private Map<UUID, LessonStatus> lessonStatusMap;
}
