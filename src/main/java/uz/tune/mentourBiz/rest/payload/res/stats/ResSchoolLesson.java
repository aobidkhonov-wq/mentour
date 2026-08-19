package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResSchoolLesson {

    private UUID lessonUuid;
    private String name;
    private Instant startTime;
    private Instant endTime;
    private String status;
    private String lessonType;
    private Long courseId;
    private String courseName;

}
