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
public class ResLowAttendanceLesson {

    private UUID lessonUuid;
    private String lessonName;
    private Instant startTime;
    private Long courseId;
    private String courseName;
    private Long present;
    private Long absent;
    private Long late;
    private Long notMarked;
    private Long total;
    private Double attendanceRate;

}
