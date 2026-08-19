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
public class ResCourseInfo {

    private UUID schoolUuid;
    private String schoolName;

    private Long courseId;
    private String courseName;
    private String courseStatus;
    private Instant courseCreatedAt;

    private String groupName;

    private Instant startDate;
    private Instant lastLessonCreatedAt;

}
