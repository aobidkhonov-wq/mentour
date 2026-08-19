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
public class ResTeacherRanking {

    private UUID teacherUuid;
    private String firstName;
    private String lastName;
    private Long totalLessons;
    private Long totalStudents;
    private Double studentAttendanceRate;
    private Double attendanceMarkingRate;
    private Long activeGroups;
    private Instant lastLessonAt;

}
