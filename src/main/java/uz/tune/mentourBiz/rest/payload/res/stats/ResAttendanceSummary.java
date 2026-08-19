package uz.tune.mentourBiz.rest.payload.res.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResAttendanceSummary {

    private Long present;
    private Long absent;
    private Long late;
    private Long notMarked;
    private Long total;
    private Double attendanceRate;

}
