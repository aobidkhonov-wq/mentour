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
public class ResEnrollmentTrend {

    private Instant month;
    private Long newEnrollments;
    private Long newStudents;
    private Long deletedEnrollments;

}
