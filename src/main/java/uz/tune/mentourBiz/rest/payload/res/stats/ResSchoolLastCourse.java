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
public class ResSchoolLastCourse {

    private UUID schoolUuid;
    private String schoolName;
    private Instant lastCourseCreatedAt;
    private Long totalCourses;

}
