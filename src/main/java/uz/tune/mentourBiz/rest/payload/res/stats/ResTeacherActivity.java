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
public class ResTeacherActivity {

    private UUID teacherUuid;
    private String firstName;
    private String lastName;
    private Long activeGroups;
    private Long totalLessons;
    private Long markedAttendances;
    private Long notMarkedAttendances;
    private Double attendanceMarkingRate;
    private Instant lastLessonAt;
    private Instant lastActiveAt;

}
