package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResTeacherAttendanceTrend {

    private Instant month;
    private Long totalLessons;
    private Long markedLessons;
    private Long unmarkedLessons;
    private Double markingRate;

}
